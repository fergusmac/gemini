import cliniko.ClinikoPatient
import com.mongodb.ClientSessionOptions
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.TransactionOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.*
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.codecs.configuration.CodecRegistries
import kotlin.reflect.KCallable

private val logger = KotlinLogging.logger {}
class ClinikoMongo (connectionString: ConnectionString, val databaseName : String) {

    val client = MongoClient.create(connectionString=connectionString)


    val codecRegistry = CodecRegistries.fromRegistries(
        CodecRegistries.fromCodecs(InstantCodec()),
        MongoClientSettings.getDefaultCodecRegistry()
    )

    val db: MongoDatabase = client.getDatabase(databaseName = databaseName).withCodecRegistry(codecRegistry)

    val patients = db.getCollection<Patient>(collectionName = "patients")


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

            patients.updateOne(
                filter = eq("_id", updated.id),
                update = updatesBson,
                options = UpdateOptions().upsert(true)
            )

            logger.info { "Finished updating patient ${updated.id} in Mongo" }
        }
    }

    suspend fun getPatient(clinikoId : Long) : Patient? {
        return patients.find(eq("cliniko.id", clinikoId)).firstOrNull()
    }

    suspend fun getPatients(clinikoIds : List<Long>) : List<Patient> {
        return patients.find(`in`("cliniko.id", clinikoIds)).toList()
    }
}



