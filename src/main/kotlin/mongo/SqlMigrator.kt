package mongo

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.Document
import java.sql.DriverManager
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}
data class PatientTransferInfo(
    val id: String,
    val customer_id: String,
    val claimant_id: String?,
    val extra_contact_id: String?,
    val events: String?,
    val manual_billing: Boolean,
    val label: String,
    val ndis_number: String?,
    val group_leader_id: String?,
    val send_claim_receipts: Boolean
)

class SqlMigrator(
    private val sqlConnectionString: String,
    private val clinikoAdapter: ClinikoMongoAdapter
) {
    fun transferPatients() = runBlocking {

        val mongo = clinikoAdapter.mongo

        DriverManager.getConnection(sqlConnectionString).use { connection ->
            val statement = connection.prepareStatement("SELECT * FROM common_models_clinikopatient")
            val resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val x = 1
                val info = PatientTransferInfo(
                    id = resultSet.getString("id"),
                    customer_id = resultSet.getString("customer_id"),
                    claimant_id = resultSet.getString("claimant_id"),
                    extra_contact_id = resultSet.getString("extra_contact_id"),
                    events = resultSet.getString("events"),
                    manual_billing = resultSet.getBoolean("manual_billing"),
                    label = resultSet.getString("label"),
                    ndis_number = resultSet.getString("ndis_number"),
                    group_leader_id = resultSet.getString("group_leader_id"),
                    send_claim_receipts = resultSet.getBoolean("send_claim_receipts")
                )

                val existing = clinikoAdapter.getPatient(info.id.toLong())
                if (existing == null) {
                    logger.error { "Could not find patient ${info.id} in mongoDB" }
                    continue
                }

                var claimant: Claimant? = null
                if (!info.claimant_id.isNullOrBlank()) {
                    val claimantAsPatient = clinikoAdapter.getPatient(info.claimant_id.toLong())

                    if (claimantAsPatient == null) {
                        logger.error { "Could not find claimant ${info.id} in mongoDB" }
                    }
                    else {
                        claimant = Claimant(
                            person = claimantAsPatient.person,
                            medicare = claimantAsPatient.medicare
                        )
                    }
                }

                val updated = existing.copy(claimant = claimant)

                val updatesMap = updated.diff(existing)

                if (updatesMap.isNullOrEmpty()) {
                    continue
                }

                logger.info { "Updating row ${updated.id} with sql row ${info.id} in Mongo" }

                mongo.upsertOne(mongo.patients, updated.id, updatesMap)

                /*    .append("customer_id", info.customer_id)
                    .append("claimant_id", info.claimant_id)
                    .append("extra_contact_id", info.extra_contact_id)
                    .append("events", info.events)
                    .append("manual_billing", info.manual_billing)
                    .append("label", info.label)
                    .append("ndis_number", info.ndis_number)
                    .append("group_leader_id", info.group_leader_id)
                    .append("send_claim_receipts", info.send_claim_receipts)
                    */
                //clinikoAdapter.
            }
        }
    }
}