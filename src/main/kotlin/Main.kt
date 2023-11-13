import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

suspend fun main(args: Array<String>) {

    println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    val patients = cliniko.getAllPatients()
    println(patients.size)
    print(patients.get(1001647897692871778)?.archivedAt?.toLocalDateTime(TimeZone.currentSystemDefault()))

}