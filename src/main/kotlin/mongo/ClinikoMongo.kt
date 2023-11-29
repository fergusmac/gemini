package mongo

import cliniko.sections.ClinikoPatient
import cliniko.sections.ClinikoPractitioner
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
        CodecRegistries.fromCodecs(InstantCodec()),
        MongoClientSettings.getDefaultCodecRegistry()
    )

    val db: MongoDatabase = client.getDatabase(databaseName = databaseName).withCodecRegistry(codecRegistry)

    val patients = db.getCollection<Patient>(collectionName = "patients")
    val practs = db.getCollection<Practitioner>(collectionName = "practs")


    suspend fun addOrUpdatePatient(clinikoPatient : ClinikoPatient) {

        client.transact { session ->

            val existing = getPatient(clinikoPatient.id)

            //copy any non-cliniko fields from the existing document (if any)
            val updated = Patient.fromCliniko(clinikoPatient, existing = existing)

            val updatesMap = updated.diff(existing)!!

            if (updatesMap.isEmpty()) {
                session.abortTransaction()
                return@transact
            }

            logger.info { "Updating patient ${updated.id} in Mongo" }

            upsertOne(patients, updated.id, updatesMap)

            logger.info { "Finished updating patient ${updated.id} in Mongo" }
        }
    }

    suspend fun addOrUpdatePract(clinikoPractitioner: ClinikoPractitioner) {

        client.transact { session ->

            val existing = getPract(clinikoPractitioner.id)

            //copy any non-cliniko fields from the existing document (if any)
            val updated = Practitioner.fromCliniko(clinikoPractitioner, existing = existing)

            val updatesMap = updated.diff(existing)!!

            if (updatesMap.isEmpty()) {
                session.abortTransaction()
                return@transact
            }

            logger.info { "Updating pract ${updated.id} in Mongo" }

            upsertOne(practs, updated.id, updatesMap)

            logger.info { "Finished updating pract ${updated.id} in Mongo" }
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

    suspend fun getPatient(clinikoId : Long) : Patient? {
        return patients.find(eq("cliniko.id", clinikoId)).firstOrNull()
    }

    suspend fun getPatients(clinikoIds : List<Long>) : List<Patient> {
        return patients.find(`in`("cliniko.id", clinikoIds)).toList()
    }

    suspend fun getPract(clinikoId : Long) : Practitioner? {
        return practs.find(eq("cliniko.id", clinikoId)).firstOrNull()
    }

    suspend fun getPracts() : List<Practitioner> {
        return practs.find().toList()
    }
}



