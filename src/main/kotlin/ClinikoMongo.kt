import cliniko.ClinikoPatient
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull
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


    suspend fun addPatient(clinikoPatient : ClinikoPatient) {
        patients.insertOne(Patient.fromCliniko(clinikoPatient))
    }

    suspend fun addPatients(clinikoPatients : Iterable<ClinikoPatient>) {
        patients.insertMany( clinikoPatients.map { Patient.fromCliniko(it) })
    }

    suspend fun getPatient(clinikoId : Long) : Patient? {
        return patients.find(eq("clinikoId", clinikoId)).firstOrNull()
    }
}