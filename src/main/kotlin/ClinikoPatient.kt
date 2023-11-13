import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date


@Serializable
data class ClinikoPatient(
    val id : Long,
    @SerialName("address_1") val address1 : String?,
    @SerialName("address_2") val address2 : String?,
    @SerialName("address_3") val address3 : String?,
    @SerialName("appointment_notes") val apptNotes : String?,
    @SerialName("archived_at") val archivedAt : Instant?,
    @SerialName("first_name") val firstName : String,
    )

@Serializable
data class ClinikoPatientMessage(val patients: List<ClinikoPatient>)


@Serializable
data class ClinikoGenericMessage(
    @SerialName("total_entries") val totalEntries: Int
)