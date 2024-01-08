import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.DriverManager
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import mongo.copyAndUpsert
import mongo.types.BillingInfo
import mongo.types.Claimant

private val logger = KotlinLogging.logger {}
data class PatientTransferInfo(
    val id: Long,
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
    val id: Long,
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

data class ApptTransferInfo(
    val id: Long,
    val dateClaimed : LocalDate?,
    val wasInvoiced : Boolean
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

            //TODO transaction
            val info = PatientTransferInfo(
                id = resultSet.getString("id").toLong(),
                customerId = resultSet.getString("customer_id"),
                claimantId = resultSet.getString("claimant_id"),
                extraContactId = resultSet.getString("extra_contact_id"),
                events = resultSet.getString("events"),
                manualBilling = resultSet.getBoolean("manual_billing"),
                ndisNumber = resultSet.getString("ndis_number"),
                groupLeaderId = resultSet.getString("group_leader_id"),
                sendClaimReceipts = resultSet.getBoolean("send_claim_receipts")
            )

            val existing = clinikoAdapter.getPatient(info.id)
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

            //TODO transaction
            val info = PractTransferInfo(
                id = resultSet.getString("id").toLong(),
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

            val existing = clinikoAdapter.getPract(info.id)
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


    fun transferAppointments() = runBlocking {

        val statement = sqlConnection.prepareStatement("SELECT * FROM common_models_clinikoappointment")
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            //TODO need to use a transaction

            var dateClaimed : LocalDate? = null
            var wasInvoiced = false

            val eventsJsonStr = resultSet.getString("events")
            if (!eventsJsonStr.isNullOrBlank()) {
                val eventsJson = Json.parseToJsonElement(eventsJsonStr) as JsonObject
                if ("sent_to_claim" in eventsJson) {
                    val datetimeStr = eventsJson["sent_to_claim"].toString()
                    val datetime = Instant.parse(datetimeStr.removeSurrounding("\""))
                    dateClaimed = datetime.toLocalDateTime(timeZone = TimeZone.UTC).date
                }
                if ("invoiced" in eventsJson) {
                    wasInvoiced = true
                }
            }

            val info = ApptTransferInfo(
                id = resultSet.getLong("id"),
                dateClaimed = dateClaimed,
                wasInvoiced = wasInvoiced
            )

            val patient = clinikoAdapter.getPatientWithAppointment(info.id)
            if (patient == null) {
                logger.error { "Could not find appointment ${info.id} in mongoDB" }
                continue
            }

            val updatedAppts = patient.appointments.copyAndUpsert(
                filtr = { it.id == info.id },
                upsertFunc = {
                    it!!.copy(
                        dateClaimed = info.dateClaimed,
                        wasInvoiced = info.wasInvoiced
                    )
                },
                requireExisting = true
            )

            val updatedPatient = patient.copy(appointments = updatedAppts)

            val updatesMap = updatedPatient.diff(patient)

            if (updatesMap.isNullOrEmpty()) {
                continue
            }

            logger.info { "Updating row ${updatedPatient.id} with sql row ${info.id} in Mongo" }

            mongo.upsertOne(mongo.patients, updatedPatient.id, updatesMap)
        }

        statement.close()

    }

}