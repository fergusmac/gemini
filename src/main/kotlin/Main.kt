import cliniko.ClinikoClient
import com.mongodb.ConnectionString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import fergusm.mongo.MongoWrapper
import fergusm.mongo.types.MongoMetadata
import java.io.File
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
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

        //every minute, load the updates from the last minute
        scheduleTask(start = LocalTime.parse("00:00:00"), interval = 1.minutes) {
            val metadata = mongo.getSingleton(mongo.metadata) ?: MongoMetadata()
            adapter.downloadUpdates(cliniko, updatesSince = metadata.lastClinikoUpdate)
        }

        //every hour, load the last three hours of updates, in case we missed something
        scheduleTask(start = LocalTime.parse("00:00:00"), interval = 1.hours) {
            val now = Clock.System.now()
            adapter.downloadUpdates(cliniko, updatesSince = now - 3.hours)
        }

        //every 24 hours, reload everything
        scheduleTask(start = LocalTime.parse("02:00:00"), interval = 24.hours) {
            adapter.downloadUpdates(cliniko, updatesSince = null)
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