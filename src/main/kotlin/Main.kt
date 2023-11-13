import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

suspend fun main(args: Array<String>) {

    println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    //val patients = cliniko.getAllPatients()
    //println(patients.size)

    val appts = cliniko.getAllAppointments()
    println(appts.size)

}