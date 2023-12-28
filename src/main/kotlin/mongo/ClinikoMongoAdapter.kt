package mongo

import cliniko.ClinikoClient
import cliniko.ClinikoRow
import cliniko.sections.*
import cliniko.instantInRange
import com.mongodb.client.model.Filters.*
import com.mongodb.kotlin.client.coroutine.MongoCollection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import mongo.types.*

private val logger = KotlinLogging.logger {}
class ClinikoMongoAdapter (val mongo : MongoWrapper) {


    fun updateAll(cliniko : ClinikoClient) {
        runBlocking {
            coroutineScope {

                val metadata = mongo.getSingleton(mongo.metadata) ?: MongoMetadata()
                val lastUpdate = metadata.lastClinikoUpdate
                val now = Clock.System.now() // this is utc

                //val filter = instantInRange(field = "updated_at", minInstant = lastUpdate, maxInstant = now)
                val filter = parametersOf() //TODO allow option to update all, regardless of time

                //download all the rows from cliniko, asynchronously
                val getPatients = async { cliniko.getPatients(params = filter) }
                val getCases = async { cliniko.getCases(params = filter) }
                val getContacts = async { cliniko.getContacts(params = filter) }
                val getAppts = async { cliniko.getAppointments(params = filter) }
                val getGroupAppts = async { cliniko.getGroupAppts(params = filter) }
                val getAttendees = async { cliniko.getAttendees(params = filter) }
                val getApptTypes = async { cliniko.getApptTypes(params = filter) }
                val getAvailabilities = async { cliniko.getAvailabilities(params = filter) }
                val getPractitioners = async { cliniko.getPractitioners(params = filter) }
                val getUsers = async { cliniko.getUsers(params = filter) }
                val getPractNumbers = async { cliniko.getPractNumbers(params = filter) }
                val getBusinesses = async { cliniko.getBusinesses(params = filter) }
                val getUnavailabilities = async { cliniko.getUnavailabilities(params = filter) }

                //getBusinesses.await().values.forEach { }
                getApptTypes.await().values.forEach { addOrUpdateApptType(it) }

                getPatients.await().values.forEach { addOrUpdatePatient(it) }
                getCases.await().values.forEach { updatePatientWithCase(it) }
                //getContacts.await().values.forEach { adapter. }
                getAppts.await().values.forEach { updatePatientWithAppt(it) }
                getAttendees.await().values.forEach { updatePatientWithAttendee(it) }
                //getGroupAppts.await().values.forEach { }

                getPractitioners.await().values.forEach { addOrUpdatePract(it) }
                getUsers.await().values.forEach { updatePractWithUser(it) }
                getPractNumbers.await().values.forEach { updatePractWithNumber(it) }

                //getAvailabilities.await().values.forEach { }
                //getUnavailabilities.await().values.forEach { }

                mongo.upsertSingleton(mongo.metadata, metadata.copy(lastClinikoUpdate = now))
            }
        }
    }

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
        mongoFieldName : String = "_id",
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

        val query = eq("appointments.id", clinikoApptId)

        return mongo.patients.find(query).firstOrNull()
    }


    suspend fun getPatient(id : Long) : Patient? = getOne(id, mongo.patients)

    suspend fun getPatients(ids : List<Long>) : List<Patient> = getMultiple(ids, mongo.patients)

    suspend fun getPract(id : Long) : Practitioner? = getOne(id, mongo.practs, fieldName = "clinikoPract.id")

    suspend fun getPractByUser(userId: Long) : Practitioner? = getOne(userId, mongo.practs, fieldName = "clinikoUser.id")

    suspend fun getPracts() : List<Practitioner> = mongo.getAll(mongo.practs)

    suspend fun getAppointmentType(id: Long) : AppointmentType? = getOne(id, mongo.apptTypes)

    suspend fun <T : Any> getOne(id : Long, collection: MongoCollection<T>, fieldName : String = "_id") : T? {
        return collection.find(eq(fieldName, id)).firstOrNull()
    }

    suspend fun <T : Any> getMultiple(ids : List<Long>, collection: MongoCollection<T>, fieldName : String = "_id") : List<T> {
        return collection.find(`in`(fieldName, ids)).toList()
    }
}



