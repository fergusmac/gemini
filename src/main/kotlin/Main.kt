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

    //val patients = cliniko.getPatients()//params = parametersOf("q[]", "id:=826332652180085831"))
    val contacts = cliniko.getContacts()

    //for (patient in patients.values) db.addOrUpdatePatient(patient)

    //val appts = cliniko.getAppointments()
    //println(appts.size)

}