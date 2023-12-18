package mongo

import cliniko.ClinikoRow
import cliniko.sections.*
import com.mongodb.client.model.Filters.*
import com.mongodb.kotlin.client.coroutine.MongoCollection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

private val logger = KotlinLogging.logger {}
class ClinikoMongoAdapter (val mongo : MongoWrapper) {

    suspend fun addOrUpdatePatient(clinikoPatient : ClinikoPatient) {

        updateRowFromCliniko(
            clinikoObj = clinikoPatient,
            collection = mongo.patients,
            updateFunc = { clinObj, mongObj -> Patient.fromCliniko(clinObj, mongObj) },
            allowCreate = true)
    }

    suspend fun updatePatientWithCase(clinikoCase: ClinikoCase) {

        updateRowFromCliniko(
            clinikoObj = clinikoCase,
            clinikoId = clinikoCase.patient.links.toId(),
            collection = mongo.patients,
            updateFunc = { clinObj, mongObj -> mongObj!!.copyCombineCase(clinObj) },
            allowCreate = false)
    }

    suspend fun updatePatientWithAppt(clinikoAppt: ClinikoAppointment) {
        updateRowFromCliniko(
            clinikoObj = clinikoAppt,
            clinikoId = clinikoAppt.patient.links.toId(),
            collection = mongo.patients,
            updateFunc = { clinObj, mongObj -> mongObj!!.copyCombineAppt(clinObj) },
            allowCreate = false)
    }

    suspend fun updatePatientWithAttendee(clinikoAttendee: ClinikoAttendee) {

        // if this is null, wont be able to find the appt on the patient to update
        if(clinikoAttendee.booking.links.toId() == null) {
            logger.error { "No booking id found on attendee ${clinikoAttendee.id}" }
            return
        }

        updateRowFromCliniko(
            clinikoObj = clinikoAttendee,
            clinikoId = clinikoAttendee.patient.links.toId(),
            collection = mongo.patients,
            updateFunc = { clinObj, mongObj -> mongObj!!.copyCombineAttendee(clinObj) },
            allowCreate = false)
    }

    suspend fun addOrUpdateApptType(clinikoApptType: ClinikoApptType) {

        updateRowFromCliniko(
            clinikoObj = clinikoApptType,
            collection = mongo.apptTypes,
            updateFunc = { clinObj, mongObj -> AppointmentType.fromCliniko(clinObj, mongObj) },
            allowCreate = true)
    }

    suspend fun addOrUpdatePract(clinikoPractitioner: ClinikoPractitioner) {

        updateRowFromCliniko(
            clinikoObj = clinikoPractitioner,
            collection = mongo.practs,
            mongoFieldName = "clinikoPract.id",
            updateFunc = { clinObj, mongObj -> Practitioner.fromCliniko(clinObj, mongObj) },
            allowCreate = true)
    }

    suspend fun updatePractWithUser(clinikoUser: ClinikoUser) {

        updateRowFromCliniko(
            clinikoObj = clinikoUser,
            collection = mongo.practs,
            mongoFieldName = "clinikoUser.id",
            updateFunc = { clinObj, mongObj -> Practitioner.combineUser(clinObj, mongObj!!) },
            allowCreate = false)
    }

    suspend fun updatePractWithNumber(clinikoNumber: ClinikoPractNumber) {

        updateRowFromCliniko(
            clinikoObj = clinikoNumber,
            clinikoId = clinikoNumber.practitioner.links.toId(),
            collection = mongo.practs,
            mongoFieldName = "clinikoUser.id",
            updateFunc = { clinObj, mongObj -> Practitioner.combineRefNumber(clinObj, mongObj!!) },
            allowCreate = false)
    }

    suspend fun <C : ClinikoRow, M : MongoRow> updateRowFromCliniko(
        clinikoObj: C,
        clinikoId : Long? = clinikoObj.id,
        collection : MongoCollection<M>,
        mongoFieldName : String = "cliniko.id",
        updateFunc : (C, M?) -> M,
        allowCreate : Boolean)
    {
        if (clinikoId == null) {
            logger.info { "Foreign id is null on cliniko row ${clinikoObj.id}, cannot update in Mongo" }
            return
        }

        mongo.client.transact { session ->

            val mongoObj = getOne(id = clinikoId, collection = collection, fieldName = mongoFieldName)

            if (!allowCreate && mongoObj == null) {
                // row hasn't yet been seen by mongo, try again later
                session.abortTransaction()
                return@transact
            }

            val updated = updateFunc(clinikoObj, mongoObj)

            val updatesMap = updated.diff(mongoObj)

            if (updatesMap.isNullOrEmpty()) {
                session.abortTransaction()
                return@transact
            }

            logger.info { "Updating row ${updated.id} with cliniko row ${clinikoObj.id} in Mongo" }

            mongo.upsertOne(collection, updated.id, updatesMap)

        }
    }

    suspend fun getPatientWithAppointment(clinikoApptId: Long): Patient? {

        val query = eq("appointments.cliniko.id", clinikoApptId)

        return mongo.patients.find(query).firstOrNull()
    }


    suspend fun getPatient(id : Long) : Patient? = getOne(id, mongo.patients)

    suspend fun getPatients(ids : List<Long>) : List<Patient> = getMultiple(ids, mongo.patients)

    suspend fun getPract(id : Long) : Practitioner? = getOne(id, mongo.practs, fieldName = "clinikoPract.id")

    suspend fun getPractByUser(userId: Long) : Practitioner? = getOne(userId, mongo.practs, fieldName = "clinikoUser.id")

    suspend fun getPracts() : List<Practitioner> = mongo.getAll(mongo.practs)

    suspend fun getAppointmentType(id: Long) : AppointmentType? = getOne(id, mongo.apptTypes)

    suspend fun <T : Any> getOne(id : Long, collection: MongoCollection<T>, fieldName : String = "id") : T? {
        return collection.find(eq(fieldName, id)).firstOrNull()
    }

    suspend fun <T : Any> getMultiple(ids : List<Long>, collection: MongoCollection<T>, fieldName : String = "id") : List<T> {
        return collection.find(`in`(fieldName, ids)).toList()
    }
}



