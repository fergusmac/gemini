import cliniko.ClinikoClient
import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import mongo.ClinikoMongoAdapter
import mongo.MongoWrapper
import mongo.SqlMigrator
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}
fun main(args: Array<String>) {

    val mongo = MongoWrapper(ConnectionString("mongodb://localhost:27017"), databaseName = "constellation")
    val adapter = ClinikoMongoAdapter(mongo)

    val localProperties = Properties().apply {
        File("./local.properties").reader().use { load(it) }
    }

    val clinikoApiKey : String by localProperties
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = clinikoApiKey)

    runBlocking {
        scheduleTask(start = LocalTime.parse("00:00:00"), interval = 1.minutes) {
            val timeZone = TimeZone.of("Australia/Sydney")
            val now = Clock.System.now().toLocalDateTime(timeZone)
            println(now)
            adapter.updateAll(cliniko)
        }
    }

    //TODO currently unreachable
    val sqlConnectionString : String  by localProperties
    val sqlMigrator = SqlMigrator(sqlConnectionString = sqlConnectionString, clinikoAdapter = adapter)
    sqlMigrator.transferPatients()
    sqlMigrator.transferPracts()
    sqlMigrator.transferAppointments()
}

suspend fun scheduleTask(start: LocalTime, interval: Duration, task : () -> Unit) {
    val timeZone = TimeZone.of("Australia/Sydney")
    val today = Clock.System.now().toLocalDateTime(timeZone).date
    var next = LocalDateTime(date = today, time = start).toInstant(timeZone)

    while (true) {
        val now = Clock.System.now()
        while (next < now) {
            next += interval
            val nextDateTime = next.toLocalDateTime(timeZone)
            if (nextDateTime.time < start) {
                // e.g. if we start at 6pm at run every hour, once we get to 1am we should stop until 6pm
                // otherwise the schedule works differently depending on when the program starts
                next = LocalDateTime(nextDateTime.date, start).toInstant(timeZone)
            }
        }
        delay(next - now)
        task()
    }
}