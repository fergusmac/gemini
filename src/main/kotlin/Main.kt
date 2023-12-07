import cliniko.ClinikoClient
import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging
import mongo.ClinikoMongoAdapter

private val logger = KotlinLogging.logger {}
suspend fun main(args: Array<String>) {


    val adapter = ClinikoMongoAdapter(ConnectionString("mongodb://localhost:27017"), databaseName = "constellation")

    //delay(10000)
    println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    val patients = cliniko.getPatients()//params = parametersOf("q[]", "id:=826332652180085831"))
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
    prns.values.forEach { adapter.updatePractWithNumber(it) }

}