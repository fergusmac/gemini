import cliniko.ClinikoClient
import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import mongo.ClinikoMongoAdapter
import mongo.MongoWrapper
import mongo.SqlMigrator
import java.io.File
import java.util.*

private val logger = KotlinLogging.logger {}
fun main(args: Array<String>) {


    val mongo = MongoWrapper(ConnectionString("mongodb://localhost:27017"), databaseName = "constellation")
    val adapter = ClinikoMongoAdapter(mongo)

    val localProperties = Properties().apply {
        File("./local.properties").reader().use { load(it) }
    }

    val clinikoApiKey : String by localProperties
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = clinikoApiKey)

    adapter.updateAll(cliniko)

    val sqlConnectionString : String  by localProperties
    val sqlMigrator = SqlMigrator(sqlConnectionString = sqlConnectionString, clinikoAdapter = adapter)
    sqlMigrator.transferPatients()
    sqlMigrator.transferPracts()
    sqlMigrator.transferAppointments()
}