package mongo

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.Document
import java.sql.DriverManager
import kotlinx.coroutines.runBlocking

data class PatientTransferInfo(
    val id: String,
    val customer_id: String,
    val claimant_id: String,
    val extra_contact_id: String,
    val events: String,
    val manual_billing: String,
    val label: String,
    val ndis_number: String,
    val group_leader_id: String,
    val send_claim_receipts: String
)

class SqlMigrator(
    private val sqlUrl: String,
    private val sqlUser: String,
    private val sqlPassword: String,
    private val mongoConnectionString: String,
    private val mongoDatabaseName: String,
    private val mongoCollectionName: String
) {
    fun transferPatient() = runBlocking {
        val client: MongoClient = MongoClient.create(connectionString = mongoConnectionString)
        val database: MongoDatabase = client.getDatabase(databaseName = mongoDatabaseName)
        val collection = database.getCollection<Patient>(collectionName = "patients")

        DriverManager.getConnection(sqlUrl, sqlUser, sqlPassword).use { connection ->
            val statement = connection.prepareStatement("SELECT * FROM common_models_clinikopatient")
            val resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val info = PatientTransferInfo(
                    id = resultSet.getString("id"),
                    customer_id = resultSet.getString("customer_id"),
                    claimant_id = resultSet.getString("claimant_id"),
                    extra_contact_id = resultSet.getString("extra_contact_id"),
                    events = resultSet.getString("events"),
                    manual_billing = resultSet.getString("manual_billing"),
                    label = resultSet.getString("label"),
                    ndis_number = resultSet.getString("ndis_number"),
                    group_leader_id = resultSet.getString("group_leader_id"),
                    send_claim_receipts = resultSet.getString("send_claim_receipts")
                )

                val document = Document("id", info.id)
                    .append("customer_id", info.customer_id)
                    .append("claimant_id", info.claimant_id)
                    .append("extra_contact_id", info.extra_contact_id)
                    .append("events", info.events)
                    .append("manual_billing", info.manual_billing)
                    .append("label", info.label)
                    .append("ndis_number", info.ndis_number)
                    .append("group_leader_id", info.group_leader_id)
                    .append("send_claim_receipts", info.send_claim_receipts)

                //collection.insertOne(document)
            }
        }
        client.close()
    }
}