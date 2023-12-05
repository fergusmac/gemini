import cliniko.ClinikoClient
import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging
import mongo.ClinikoMongo

private val logger = KotlinLogging.logger {}
suspend fun main(args: Array<String>) {


    val db = ClinikoMongo(ConnectionString("mongodb://localhost:27017"), databaseName = "constellation")

    //delay(10000)
    println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    val patients = cliniko.getPatients()//params = parametersOf("q[]", "id:=826332652180085831"))
    val cases = cliniko.getCases()
    //cliniko.getContacts()
    val appts = cliniko.getAppointments()
    /*cliniko.getGroupAppts()

    cliniko.getUnavailabilities()
    cliniko.getAttendees()
    cliniko.getApptTypes()
    cliniko.getAvailabilities()
    cliniko.getGroupAppts()*/
    val practs = cliniko.getPractitioners()
    val users = cliniko.getUsers()
    val prns = cliniko.getPractNumbers()
    //cliniko.getBusinesses()


    patients.values.forEach { db.addOrUpdatePatient(it) }
    cases.values.forEach { db.updatePatientWithCase(it) }
    appts.values.forEach { db.updatePatientWithAppt(it) }
    for (pract in practs.values) db.addOrUpdatePract(pract)
    for (user in users.values) db.updatePractWithUser(user)
    for (prn in prns.values) db.updatePractWithNumber(prn)

}