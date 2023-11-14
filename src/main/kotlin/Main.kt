import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val logger = KotlinLogging.logger {}
suspend fun main(args: Array<String>) {


    val translator = ClinikoTranslator(ConnectionString("mongodb://localhost:27017"), databaseName = "constellation")

    logger.info { "Test log line" }

    //delay(10000)
    /*println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    val patients = cliniko.getPatients()

    translator.addPatient(patients.values.first())

    val patient = translator.getPatient(826330779775671358)
    println(patient?.name.toString())

    //val appts = cliniko.getAppointments()
    //println(appts.size)*/

}