import cliniko.ClinikoPatient
import com.mongodb.ConnectionString
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull

private val logger = KotlinLogging.logger {}
class ClinikoTranslator (connectionString: ConnectionString, val databaseName : String) {

    val client = MongoClient.create(connectionString=connectionString)
    val db: MongoDatabase = client.getDatabase(databaseName = databaseName)

    val patients = db.getCollection<Patient>(collectionName = "patients")

    suspend fun addPatient(clinikoPatient : ClinikoPatient) {
        val patient = Patient.fromCliniko(clinikoPatient)

        patients.insertOne(patient)
    }

    suspend fun getPatient(clinikoId : Long) : Patient? {
        return patients.find(eq("clinikoId", clinikoId)).firstOrNull()
    }
}