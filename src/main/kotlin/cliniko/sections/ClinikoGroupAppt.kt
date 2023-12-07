package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SECTION_GROUP_APPTS = "group_appointments"

@Serializable
data class ClinikoGroupAppt(
    override val id : Long,
    @SerialName("archived_at") val archivedAt : Instant?,
    val business : LinkField,
    @SerialName("created_at") val createdAt : Instant?,
    @SerialName("ends_at") val endsAt: Instant,
    @SerialName("max_attendees") val maxAttendees: Int,
    val notes: String?,
    @SerialName("patient_ids") val patientIds : List<Long>?,
    val practitioner: LinkField,
    @SerialName("starts_at") val startsAt: Instant,
    @SerialName("updated_at") val updatedAt : Instant?,
    @SerialName("telehealth_url") val telehealthUrl : String?
) : ClinikoRow

@Serializable
data class ClinikoGroupApptMessage(@SerialName(SECTION_GROUP_APPTS) val appointments: List<ClinikoGroupAppt>)

