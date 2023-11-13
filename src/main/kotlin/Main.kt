import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

suspend fun main(args: Array<String>) {

    println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    val patients = cliniko.getAllPatients()
    println(patients.size)

}