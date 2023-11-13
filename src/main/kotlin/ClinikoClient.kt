import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.time.Duration.Companion.minutes


const val RESULTS_PER_PAGE = 100
const val SECTION_PATIENTS = "patients"

class ClinikoClient(val baseUrl: String, apiKey: String) {

    val rateLimiter = RateLimiter(limit = 100, interval = 1.minutes)

    val client = HttpClient(CIO) {
        expectSuccess = true

        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(username = apiKey, password = "")
                }
                // send authorization on first request (without waiting for denial from server)
                sendWithoutRequest { request ->
                    true
                }
            }
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
    }

    suspend fun getRaw(pathSegments: List<String>, params : Parameters = parametersOf()) : String {

        val urlBuilder = URLBuilder(
            host = baseUrl,
            pathSegments = pathSegments,
            parameters = params,
            protocol = URLProtocol.HTTPS
        )

        var response : HttpResponse? = null

        rateLimiter.submitTask {
            response = client.get(urlBuilder.build()) {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.ContentType, "application/json")
                    append(HttpHeaders.UserAgent, "Gemini (fdmacpherson@gmail.com)")
                }
            }
        }

        return response!!.bodyAsText()
    }

    suspend fun getPages(pathSegments: List<String>, params : Parameters = parametersOf()) : List<String> {

        val parameters = params + parametersOf("per_page", RESULTS_PER_PAGE.toString())

        var currentPage = 1
        var finalPage = 1

        val responsePages = mutableListOf<String>()

        while(currentPage <= finalPage) {
            val pageParams = parameters + parametersOf("page", currentPage.toString())

            responsePages += getRaw(pathSegments=pathSegments, params=pageParams)

            //TODO use coroutines for parallel requests
            if(currentPage == 1) {
                //parse the first message, so we can read how many entries there are to fetch in total
                val msg = parseJson<ClinikoGenericMessage>(responsePages.first())
                finalPage = ceil(msg.totalEntries / RESULTS_PER_PAGE.toFloat()).toInt()
            }

            currentPage++
        }

        return responsePages
    }

    suspend fun getAllPatients() : Map<Long, ClinikoPatient> {

        //TODO get archived as well
        val pages = getPages(listOf("v1", SECTION_PATIENTS))

        val patients = mutableMapOf<Long, ClinikoPatient>()
        for (page in pages) {
            val msg = parseJson<ClinikoPatientMessage>(page)
            patients.putAll(msg.patients.associateBy { it.id })
        }


        return patients
    }
}

inline fun <reified T> parseJson(jsonStr: String) : T {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    return json.decodeFromString<T>(jsonStr)
}