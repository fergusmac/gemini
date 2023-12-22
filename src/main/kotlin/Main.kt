import cliniko.ClinikoClient
import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import mongo.ClinikoMongoAdapter
import mongo.MongoWrapper
import mongo.SqlMigrator
import java.io.File
import java.util.*

private val logger = KotlinLogging.logger {}
fun main(args: Array<String>) {


    val mongo = MongoWrapper(ConnectionString("mongodb://localhost:27017"), databaseName = "constellation")
    val adapter = ClinikoMongoAdapter(mongo)

    val localProperties = Properties().apply {
        File("./local.properties").reader().use { load(it) }
    }

    val clinikoApiKey : String by localProperties
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = clinikoApiKey)

    runBlocking {
        coroutineScope {

            //download all the rows from cliniko, asynchronously
            val getPatients = async { cliniko.getPatients() }
            val getCases = async { cliniko.getCases() }
            val getContacts = async { cliniko.getContacts() }
            val getAppts = async { cliniko.getAppointments() }
            val getGroupAppts = async { cliniko.getGroupAppts() }
            val getAttendees = async { cliniko.getAttendees() }
            val getApptTypes = async { cliniko.getApptTypes() }
            val getAvailabilities = async { cliniko.getAvailabilities() }
            val getPractitioners = async { cliniko.getPractitioners() }
            val getUsers = async { cliniko.getUsers() }
            val getPractNumbers = async { cliniko.getPractNumbers() }
            val getBusinesses = async { cliniko.getBusinesses() }
            val getUnavailabilities = async { cliniko.getUnavailabilities() }

            //getBusinesses.await().values.forEach { }
            getApptTypes.await().values.forEach { adapter.addOrUpdateApptType(it) }

            getPatients.await().values.forEach { adapter.addOrUpdatePatient(it) }
            getCases.await().values.forEach { adapter.updatePatientWithCase(it) }
            //getContacts.await().values.forEach { adapter. }
            getAppts.await().values.forEach { adapter.updatePatientWithAppt(it) }
            getAttendees.await().values.forEach { adapter.updatePatientWithAttendee(it) }
            //getGroupAppts.await().values.forEach { }

            getPractitioners.await().values.forEach { adapter.addOrUpdatePract(it) }
            getUsers.await().values.forEach { adapter.updatePractWithUser(it) }
            getPractNumbers.await().values.forEach { adapter.updatePractWithNumber(it) }

            //getAvailabilities.await().values.forEach { }
            //getUnavailabilities.await().values.forEach { }
        }
    }

    val sqlConnectionString : String  by localProperties
    val sqlMigrator = SqlMigrator(sqlConnectionString = sqlConnectionString, clinikoAdapter = adapter)
    sqlMigrator.transferPatients()
    sqlMigrator.transferPracts()
    sqlMigrator.transferAppointments()
}