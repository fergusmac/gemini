import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*


const val RESULTS_PER_PAGE = 100

class ClinikoClient (val baseUrl : String, apiKey: String){

    val client = HttpClient(CIO) {
        expectSuccess = true

        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(username = apiKey, password = "")
                }
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

    suspend fun getSection(section: String, params: Map<String, String> = emptyMap(), subsection : String = "") {

        val pathSegments = mutableListOf("v1", section)
        if (subsection.isNotEmpty()) pathSegments += subsection

        //Paramters class allows multiple values per key, but we pass in just a single value per key, re-map here
        var parameters = parametersOf(params.mapValues { listOf(it.value) })
        parameters += parametersOf("per_page", RESULTS_PER_PAGE.toString())

        var page = 1

        val pageParameters = parameters + parametersOf("page", page.toString())
        val urlBuilder = URLBuilder(host=baseUrl, pathSegments = pathSegments, parameters = pageParameters, protocol = URLProtocol.HTTPS)

        val response = client.get(urlBuilder.build()) {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.ContentType, "application/json")
                append(HttpHeaders.UserAgent, "Gemini (fdmacpherson@gmail.com)")
            }
        }

        print(response.status)
    }
}