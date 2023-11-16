import cliniko.ClinikoPatient
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.codecs.configuration.CodecRegistries

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

        val query = Filters.eq(Patient::cliniko.name + "." + ClinikoObject::id.name, clinikoPatient.id)
        val updates = Updates.combine(
            Updates.set(Patient::marketingSource.name, clinikoPatient.referralSource),
            //TODO rest of the fields
            /*Updates.addToSet(Movie::genres.name, "Sports"),
            Updates.currentDate(Movie::lastUpdated.name)*/
        )
        val options = UpdateOptions().upsert(true)

        try {
            patients.updateOne(query, updates, options)
        } catch (e: MongoException) {
            logger.error { "Unable to update cliniko patient ${clinikoPatient.id} due to an error: $e" }
        }
    }

    suspend fun getPatient(clinikoId : Long) : Patient? {
        return patients.find(eq("cliniko.id", clinikoId)).firstOrNull()
    }

    suspend fun getPatients(clinikoIds : List<Long>) : List<Patient> {
        return patients.find(`in`("cliniko.id", clinikoIds)).toList()
    }
}

