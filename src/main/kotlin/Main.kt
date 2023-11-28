import cliniko.ClinikoClient
import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}
suspend fun main(args: Array<String>) {


    val db = ClinikoMongo(ConnectionString("mongodb://localhost:27017"), databaseName = "constellation")

    logger.info { "Test log line" }

    //delay(10000)
    println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    cliniko.getPatients()//params = parametersOf("q[]", "id:=826332652180085831"))
    cliniko.getContacts()
    cliniko.getAppointments()
    cliniko.getGroupAppts()
    cliniko.getUsers()
    cliniko.getUnavailabilities()
    cliniko.getAttendees()
    cliniko.getApptTypes()
    cliniko.getAvailabilities()
    cliniko.getGroupAppts()
    cliniko.getPractitioners()
    cliniko.getPractNumbers()
    cliniko.getBusinesses()

    //for (patient in patients.values) db.addOrUpdatePatient(patient)

    //val appts = cliniko.getAppointments()
    //println(appts.size)

}