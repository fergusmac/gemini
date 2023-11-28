package cliniko

import RateLimiter
import cliniko.sections.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.reflect.KProperty1
import kotlin.time.Duration.Companion.minutes


const val RESULTS_PER_PAGE = 100


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
        val responsePages = mutableListOf<String>()

        responsePages += getRaw(pathSegments=pathSegments, params=parameters)

        //parse the first page, so we can read how many entries there are to fetch in total
        val msg = parseJson<ClinikoGenericMessage>(responsePages.first())

        val lastPage = ceil(msg.totalEntries / RESULTS_PER_PAGE.toFloat()).toInt()

        //get the rest of the pages in parallel
        responsePages += coroutineScope {
            (2 .. lastPage).map {
                async {
                    val pageParams = parameters + parametersOf("page", it.toString())
                    getRaw(pathSegments=pathSegments, params=pageParams)
                }
            }.awaitAll()
        }

        return responsePages
    }

    suspend fun getPatients(params: Parameters = parametersOf()) : Map<Long, ClinikoPatient> {

        return getSection(
            section= SECTION_PATIENTS,
            itemsProp= ClinikoPatientMessage::patients,
            params=params + wildcardParam("archived_at")
        ).associateBy { it.id }
    }

    suspend fun getContacts(params: Parameters = parametersOf()) : Map<Long, ClinikoContact> {

        return getSection(
            section= SECTION_CONTACTS,
            itemsProp= ClinikoContactMessage::contacts,
            params=params + wildcardParam("archived_at")
        ).associateBy { it.id }
    }

    suspend fun getCases(params: Parameters = parametersOf()) : Map<Long, ClinikoCase> {

        return getSection(
            section= SECTION_CASES,
            itemsProp= ClinikoCaseMessage::patientCases,
            params=params + wildcardParam("archived_at")
        ).associateBy { it.id }
    }

    suspend fun getAppointments(params: Parameters = parametersOf()) : Map<Long, ClinikoAppointment> {
        //this is for individual appts only

        return getSection(
            section= SECTION_APPOINTMENTS,
            itemsProp= ClinikoAppointmentMessage::appointments,
            params=params + wildcardParam("archived_at") + wildcardParam("cancelled_at")
        ).associateBy { it.id }
    }

    suspend fun getApptTypes(params: Parameters = parametersOf()) : Map<Long, ClinikoApptType> {
        return getSection(
            section= SECTION_APPT_TYPES,
            itemsProp= ClinikoApptTypeMessage::apptTypes,
            params=params + wildcardParam("archived_at")
        ).associateBy { it.id }
    }

    suspend fun getAttendees(params: Parameters = parametersOf()) : Map<Long, ClinikoAttendee> {
        return getSection(
            section= SECTION_ATTENDEES,
            itemsProp= ClinikoAttendeeMessage::attendees,
            params=params + wildcardParam("archived_at") + wildcardParam("cancelled_at")
        ).associateBy { it.id }
    }

    suspend fun getBusinesses(params: Parameters = parametersOf()) : Map<Long, ClinikoBusiness> {
        return getSection(
            section= SECTION_BUSINESSES,
            itemsProp= ClinikoBusinessMessage::businesses,
            params=params + wildcardParam("archived_at")
        ).associateBy { it.id }
    }

    suspend fun getAvailabilities(params: Parameters = parametersOf()) : Map<Long, ClinikoAvailability> {
        return getSection(
            section= SECTION_AVAILABILITY,
            itemsProp= ClinikoAvailabilityMessage::availabilities,
            params=params
        ).associateBy { it.id }
    }

    suspend fun getPractitioners(params: Parameters = parametersOf()) : Map<Long, ClinikoPractitioner> {
        return getSection(
            section= SECTION_PRACTITIONERS,
            itemsProp= ClinikoPractMessage::practitioners,
            params=params
        ).associateBy { it.id }
    }

    suspend fun getPractNumbers(params: Parameters = parametersOf()) : Map<Long, ClinikoPractNumber> {
        return getSection(
            section= SECTION_PRACT_NUMBERS,
            itemsProp= ClinikoPractNumMessage::numbers,
            params=params
        ).associateBy { it.id }
    }

    suspend fun getUsers(params: Parameters = parametersOf()) : Map<Long, ClinikoUser> {
        return getSection(
            section= SECTION_USERS,
            itemsProp= ClinikoUserMessage::users,
            params=params
        ).associateBy { it.id }
    }

    suspend fun getUnavailabilities(params: Parameters = parametersOf()) : Map<Long, ClinikoUnavailability> {
        return getSection(
            section= SECTION_UNAVAILABILITIES,
            itemsProp= ClinikoUnavailabilityMessage::unavailabilities,
            params=params + wildcardParam("deleted_at")
        ).associateBy { it.id }
    }

    suspend fun getGroupAppts(params: Parameters = parametersOf()) : Map<Long, ClinikoGroupAppt> {
        return getSection(
            section= SECTION_GROUP_APPTS,
            itemsProp= ClinikoGroupApptMessage::appointments,
            params=params + wildcardParam("archived_at")
        ).associateBy { it.id }
    }

    suspend inline fun <T, reified Msg> getSection(section : String, itemsProp : KProperty1<Msg, List<T>>, params: Parameters = parametersOf()) : List<T> {
        val pages = getPages(listOf("v1", section), params = params)

        val results = mutableListOf<T>()
        for (page in pages) {
            val msg = parseJson<Msg>(page)
            results.addAll(itemsProp.get(msg))
        }

        return results
    }
}

inline fun <reified T> parseJson(jsonStr: String) : T {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    return json.decodeFromString<T>(jsonStr)
}

fun wildcardParam(name: String) : Parameters {
    return parametersOf("q[]", "$name:*")
}
