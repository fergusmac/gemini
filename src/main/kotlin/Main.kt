suspend fun main(args: Array<String>) {

    println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    cliniko.getSection("patients")
}