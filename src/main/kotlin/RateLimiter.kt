import kotlinx.coroutines.sync.Semaphore
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.TimeSource

class RateLimiter(private val limit: Int, private val interval: Duration) {

    private val timeSource = TimeSource.Monotonic
    private val semaphore = Semaphore(limit)
    private val tickMillis = ceil( interval.inWholeMilliseconds / limit.toFloat()).toLong()
    private var timer : Timer? = null
    private var lastRelease = timeSource.markNow()

    suspend fun submitTask(task: suspend () -> Unit) {
        //if timer is inactive, start it
        if(timer == null) unpause()
        semaphore.acquire() // Wait until a permit is available
        task()
    }

    private fun releasePermit() {
        if(semaphore.availablePermits < limit)
        {
            semaphore.release()
            lastRelease = timeSource.markNow()
        }

        if((timeSource.markNow() - lastRelease > interval) and (semaphore.availablePermits == limit)) {
            //if all permits are released, and it's been 1 interval since we released the last one, pause the timer
            pause()
        }
    }

    private fun pause() {
        timer?.cancel()
        timer = null
    }

    private fun unpause() {
        assert(timer == null)
        timer = fixedRateTimer("ReleasePermitTimer", true, 0, tickMillis) {
            releasePermit()
        }
    }
}