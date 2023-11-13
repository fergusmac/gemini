import cliniko_messages.ClinikoPatient
import com.mongodb.ConnectionString
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonInt64
import org.bson.Document
import org.bson.types.ObjectId

class ClinikoTranslator (connectionString: ConnectionString, val databaseName : String) {

    val client = MongoClient.create(connectionString=connectionString)
    val db: MongoDatabase = client.getDatabase(databaseName = databaseName)

    val patients = db.getCollection<Patient>(collectionName = "patients")

    suspend fun addPatient(clinikoPatient : ClinikoPatient) {
        val patient = Patient(
            id = ObjectId(),
            clinikoId = clinikoPatient.id,
            name = Name(
                first = clinikoPatient.firstName,
                preferred = clinikoPatient.preferredFirstName,
                last = clinikoPatient.lastName
            )
        )

        patients.insertOne(patient).also { println("Inserted ${it.insertedId}")}
    }

    suspend fun getPatient(clinikoId : Long) : Patient? {
        return patients.find(eq("clinikoId", clinikoId)).firstOrNull()
    }
}