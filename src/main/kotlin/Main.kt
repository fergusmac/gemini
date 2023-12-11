import cliniko.ClinikoClient
import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging
import mongo.ClinikoMongoAdapter
import mongo.MongoWrapper
import mongo.SqlMigrator
import java.io.File
import java.util.*

private val logger = KotlinLogging.logger {}
suspend fun main(args: Array<String>) {


    val mongo = MongoWrapper(ConnectionString("mongodb://localhost:27017"), databaseName = "constellation")
    val adapter = ClinikoMongoAdapter(mongo)

    val localProperties = Properties().apply {
        File("./local.properties").reader().use { load(it) }
    }

    //delay(10000)
    val clinikoApiKey : String by localProperties
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = clinikoApiKey)

    /*val patients = cliniko.getPatients()//params = parametersOf("q[]", "id:=826332652180085831"))
    val cases = cliniko.getCases()
    //cliniko.getContacts()
    val appts = cliniko.getAppointments()
    //cliniko.getGroupAppts()

    //cliniko.getUnavailabilities()
    val attendees = cliniko.getAttendees()
    /*cliniko.getApptTypes()
    cliniko.getAvailabilities()
    cliniko.getGroupAppts()*/
    val practs = cliniko.getPractitioners()
    val users = cliniko.getUsers()
    val prns = cliniko.getPractNumbers()
    //cliniko.getBusinesses()


    patients.values.forEach { adapter.addOrUpdatePatient(it) }
    cases.values.forEach { adapter.updatePatientWithCase(it) }
    appts.values.forEach { adapter.updatePatientWithAppt(it) }
    attendees.values.forEach { adapter.updatePatientWithAttendee(it) }
    practs.values.forEach { adapter.addOrUpdatePract(it) }
    users.values.forEach { adapter.updatePractWithUser(it) }
    prns.values.forEach { adapter.updatePractWithNumber(it) }*/

    val sqlConnectionString : String  by localProperties
    val sqlMigrator = SqlMigrator(sqlConnectionString = sqlConnectionString, clinikoAdapter = adapter)
    sqlMigrator.transferPatients()
    sqlMigrator.transferPracts()
}