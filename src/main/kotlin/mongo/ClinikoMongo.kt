package mongo

import Diffable
import cliniko.ClinikoRow
import cliniko.sections.*
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.*
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

private val logger = KotlinLogging.logger {}
class ClinikoMongo (connectionString: ConnectionString, val databaseName : String) {

    val client = MongoClient.create(connectionString=connectionString)


    val codecRegistry = CodecRegistries.fromRegistries(
        CodecRegistries.fromCodecs(
            InstantCodec(),
            EnumCodec.buildCodec<CancellationType>(),
            EnumCodec.buildCodec<PractitionerKind>()
        ),
        MongoClientSettings.getDefaultCodecRegistry()
    )

    val db: MongoDatabase = client.getDatabase(databaseName = databaseName).withCodecRegistry(codecRegistry)

    val patients = db.getCollection<Patient>(collectionName = "patients")
    val practs = db.getCollection<Practitioner>(collectionName = "practs")
    val apptTypes = db.getCollection<AppointmentType>(collectionName = "apptTypes")

    suspend fun addOrUpdatePatient(clinikoPatient : ClinikoPatient) {

        updateRowFromCliniko(
            clinikoObj = clinikoPatient,
            collection = patients,
            updateFunc = { clinObj, mongObj -> Patient.fromCliniko(clinObj, mongObj) },
            allowCreate = true)
    }

    suspend fun updatePatientWithCase(clinikoCase: ClinikoCase) {

        updateRowFromCliniko(
            clinikoObj = clinikoCase,
            clinikoId = clinikoCase.patient.links.toId(),
            collection = patients,
            updateFunc = { clinObj, mongObj -> Patient.combineCase(clinObj, mongObj!!) },
            allowCreate = false)
    }

    suspend fun updatePatientWithAppt(clinikoAppt: ClinikoAppointment) {
        updateRowFromCliniko(
            clinikoObj = clinikoAppt,
            clinikoId = clinikoAppt.patient.links.toId(),
            collection = patients,
            updateFunc = { clinObj, mongObj -> Patient.combineAppt(clinObj, mongObj!!) },
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
            collection = patients,
            updateFunc = { clinObj, mongObj -> Patient.combineAttendee(clinObj, mongObj!!) },
            allowCreate = false)
    }

    suspend fun addOrUpdateApptType(clinikoApptType: ClinikoApptType) {

        updateRowFromCliniko(
            clinikoObj = clinikoApptType,
            collection = apptTypes,
            updateFunc = { clinObj, mongObj -> AppointmentType.fromCliniko(clinObj, mongObj) },
            allowCreate = true)
    }

    suspend fun addOrUpdatePract(clinikoPractitioner: ClinikoPractitioner) {

        updateRowFromCliniko(
            clinikoObj = clinikoPractitioner,
            collection = practs,
            mongoFieldName = "clinikoPract.id",
            updateFunc = { clinObj, mongObj -> Practitioner.fromCliniko(clinObj, mongObj) },
            allowCreate = true)
    }

    suspend fun updatePractWithUser(clinikoUser: ClinikoUser) {

        updateRowFromCliniko(
            clinikoObj = clinikoUser,
            collection = practs,
            mongoFieldName = "clinikoUser.id",
            updateFunc = { clinObj, mongObj -> Practitioner.combineUser(clinObj, mongObj!!) },
            allowCreate = false)
    }

    suspend fun updatePractWithNumber(clinikoNumber: ClinikoPractNumber) {

        updateRowFromCliniko(
            clinikoObj = clinikoNumber,
            clinikoId = clinikoNumber.practitioner.links.toId(),
            collection = practs,
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

        client.transact { session ->

            val mongoObj = getOne(clinikoId = clinikoId, collection = collection, fieldName = mongoFieldName)

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

            upsertOne(patients, updated.id, updatesMap)

        }
    }

    suspend fun <T : Any> upsertOne(collection : MongoCollection<T>, id : ObjectId, updatesMap: Map<String, Any?>) {
        val updatesBson = combine(
            updatesMap.map {
                if(it.value == null) {
                    unset(it.key)
                }
                else {
                    set(it.key, it.value)
                }
            }
        )

        println(updatesBson) //TODO

        collection.updateOne(
            filter = eq("_id", id),
            update = updatesBson,
            options = UpdateOptions().upsert(true)
        )
    }

    suspend fun getPatient(clinikoId : Long) : Patient? = getOne(clinikoId, patients)

    suspend fun getPatients(clinikoIds : List<Long>) : List<Patient> = getMultiple(clinikoIds, patients)

    suspend fun getPract(clinikoId : Long) : Practitioner? = getOne(clinikoId, practs, fieldName = "clinikoPract.id")

    suspend fun getPractByUser(clinikoUserId: Long) : Practitioner? = getOne(clinikoUserId, practs, fieldName = "clinikoUser.id")

    suspend fun getPracts() : List<Practitioner> = getAll(practs)

    suspend fun getAppointmentType(clinikoId: Long) : AppointmentType? = getOne(clinikoId, apptTypes)

    suspend fun <T : Any> getOne(clinikoId : Long, collection: MongoCollection<T>, fieldName : String = "cliniko.id") : T? {
        return collection.find(eq(fieldName, clinikoId)).firstOrNull()
    }

    suspend fun <T : Any> getMultiple(clinikoIds : List<Long>, collection: MongoCollection<T>, fieldName : String = "cliniko.id") : List<T> {
        return collection.find(`in`(fieldName, clinikoIds)).toList()
    }

    suspend fun <T : Any> getAll(collection: MongoCollection<T>) : List<T> {
        return collection.find().toList()
    }
}



