import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

suspend fun main(args: Array<String>) {

    /*println("Api Key: ")
    val cliniko = ClinikoClient("api.au3.cliniko.com", apiKey = readln())

    cliniko.getSection("patients")*/

    // Example usage
    val rateLimiter = RateLimiter(5, 1.seconds)

    // Simulate function calls
    repeat(20) { i ->
        rateLimiter.submitTask {
            println("Executing task $i ${Date()}")
            // Your function logic goes here
        }
    }

    // Allow some time for tasks to complete
    delay(10000)

    repeat(20) { i ->
        rateLimiter.submitTask {
            println("Executing task $i ${Date()}")
            // Your function logic goes here
        }
    }

}