import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

open class ClinikoMessage() {

    
}

@Serializable
data class ClinikoPatient(
    val id : Long,
    @SerialName("first_name") val firstName : String,
    )


@Serializable
data class ClinikoPatientMessage(val patients: List<ClinikoPatient>) : ClinikoMessage()


@Serializable
data class ClinikoGenericMessage(
    @SerialName("total_entries") val totalEntries: Int
)