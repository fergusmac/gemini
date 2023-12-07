package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


const val SECTION_CASES = "patient_cases"

@Serializable
class ClinikoCase (
    override val id : Long,
    @SerialName("archived_at") val archivedAt : Instant?,
    @SerialName("created_at") val createdAt : Instant,
    @SerialName("updated_at") val updatedAt : Instant,
    val closed: Boolean,
    val contact: LinkField? = null,
    @SerialName("expiry_date") val expiryDate : LocalDate?,
    @SerialName("issue_date") val issueDate : LocalDate?,
    @SerialName("max_sessions") val maxSessions : Int?,
    val name: String,
    val patient: LinkField,
    val referral: Boolean,
    @SerialName("referral_type") val referralType : String? //"dva", "medicare" or null
) : ClinikoRow

@Serializable
data class ClinikoCaseMessage(@SerialName(SECTION_CASES) val patientCases: List<ClinikoCase>)
