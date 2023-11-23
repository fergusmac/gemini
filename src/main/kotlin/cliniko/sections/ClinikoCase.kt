package cliniko.sections

import cliniko.LinkField
import cliniko.PhoneNumber
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


const val SECTION_CASES = "patient_cases"

@Serializable
class ClinikoCase (
    val id : Long,
    @SerialName("archived_at") val archivedAt : Instant?,
    @SerialName("created_at") val createdAt : Instant,
    @SerialName("updated_at") val updatedAt : Instant,
    val closed: Boolean,
    val contact: LinkField,
    @SerialName("expiry_date") val expiryDate : LocalDate?,
    @SerialName("issue_date") val issueDate : LocalDate?,
    @SerialName("max_sessions") val maxSessions : Int?,
    val name: String,
    val patient: LinkField,
    val referral: Boolean,
    @SerialName("referral_type") val referralType : String? //"dva", "medicare" or null
)

@Serializable
data class ClinikoCaseMessage(val patient_cases: List<ClinikoCase>)
