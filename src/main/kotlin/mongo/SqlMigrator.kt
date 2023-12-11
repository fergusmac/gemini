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
    val customerId: String,
    val claimantId: String?,
    val extraContactId: String?,
    val events: String?,
    val manualBilling: Boolean,
    val ndisNumber: String?,
    val groupLeaderId: String?,
    val sendClaimReceipts: Boolean
)

data class PractTransferInfo(
    val id: String,
    val gpLetterTemplateId : String,
    val gpLetterFolderId : String,
    val acceptingIntakes : Boolean,
    val stripeAccount : String,
    val houseFeePercent : Int,
    val nextFeeInvoiceId : Int,
    val standardPriceDollars : Int,
    val xeroContactId : String?,
    val edpInitialTemplateId : String,
)

class SqlMigrator(
    sqlConnectionString: String,
    private val clinikoAdapter: ClinikoMongoAdapter
) {

    val sqlConnection = DriverManager.getConnection(sqlConnectionString)
    val mongo = clinikoAdapter.mongo

    fun transferPatients() = runBlocking {

        val resultSet = sqlConnection.prepareStatement("SELECT * FROM common_models_clinikopatient").executeQuery()

        while (resultSet.next()) {
            val info = PatientTransferInfo(
                id = resultSet.getString("id"),
                customerId = resultSet.getString("customer_id"),
                claimantId = resultSet.getString("claimant_id"),
                extraContactId = resultSet.getString("extra_contact_id"),
                events = resultSet.getString("events"),
                manualBilling = resultSet.getBoolean("manual_billing"),
                ndisNumber = resultSet.getString("ndis_number"),
                groupLeaderId = resultSet.getString("group_leader_id"),
                sendClaimReceipts = resultSet.getBoolean("send_claim_receipts")
            )

            val existing = clinikoAdapter.getPatient(info.id.toLong())
            if (existing == null) {
                logger.error { "Could not find patient ${info.id} in mongoDB" }
                continue
            }

            var claimant: Claimant? = null
            if (!info.claimantId.isNullOrBlank()) {
                val claimantAsPatient = clinikoAdapter.getPatient(info.claimantId.toLong())

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

            val billingInfo = BillingInfo(
                customerId = info.customerId,
                billingContact = null,
                manualBilling = info.manualBilling)

            val updated = existing.copy(claimant = claimant,
                billingInfo = billingInfo,
                ndisNumber = info.ndisNumber?.toLong())

            val updatesMap = updated.diff(existing)

            if (updatesMap.isNullOrEmpty()) {
                continue
            }

            logger.info { "Updating row ${updated.id} with sql row ${info.id} in Mongo" }

            mongo.upsertOne(mongo.patients, updated.id, updatesMap)
        }
    }

    fun transferPracts() = runBlocking {

        val resultSet = sqlConnection.prepareStatement("SELECT * FROM common_models_clinikopractitioner").executeQuery()

        while (resultSet.next()) {
            val info = PractTransferInfo(
                id = resultSet.getString("id"),
                gpLetterTemplateId = resultSet.getString("gp_letter_template_id"),
                gpLetterFolderId = resultSet.getString("gp_letter_folder_id"),
                acceptingIntakes = resultSet.getBoolean("accepting_intakes"),
                stripeAccount = resultSet.getString("stripe_account"),
                houseFeePercent = resultSet.getInt("house_fee_percent"),
                nextFeeInvoiceId = resultSet.getInt("next_fee_invoice_id"),
                standardPriceDollars = resultSet.getInt("standard_price_dollars"),
                xeroContactId = resultSet.getString("xero_contact_id"),
                edpInitialTemplateId = resultSet.getString("edp_initial_template_id")
            )

            val existing = clinikoAdapter.getPract(info.id.toLong())
            if (existing == null) {
                logger.error { "Could not find practitioner ${info.id} in mongoDB" }
                continue
            }

            var letterTemplates : MutableMap<String, String> = mutableMapOf()

            if (info.gpLetterTemplateId.isNotBlank()) {
                letterTemplates["default"] = info.gpLetterTemplateId
            }

            if (info.edpInitialTemplateId.isNotBlank()) {
                letterTemplates["edpInitial"] = info.edpInitialTemplateId
            }

            val updated = existing.copy(
                lettersFolderId = info.gpLetterFolderId,
                isTakingIntakes = info.acceptingIntakes,
                stripeAccount = info.stripeAccount,
                houseFeePercent = info.houseFeePercent,
                nextServiceFeeId = info.nextFeeInvoiceId,
                standardPriceDollars = info.standardPriceDollars,
                xeroContactId = info.xeroContactId,
                letterTemplates = letterTemplates
            )

            val updatesMap = updated.diff(existing)

            if (updatesMap.isNullOrEmpty()) {
                continue
            }

            logger.info { "Updating row ${updated.id} with sql row ${info.id} in Mongo" }

            mongo.upsertOne(mongo.practs, updated.id, updatesMap)
        }
    }

}