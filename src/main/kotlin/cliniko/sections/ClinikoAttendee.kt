package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SECTION_ATTENDEES = "attendees"

@Serializable
class ClinikoAttendee (
    override val id : Long,
    @SerialName("archived_at") val archivedAt : Instant?,
    val arrived : Boolean?,
    val booking: LinkField,
    @SerialName("booking_ip_address") val bookingIpAddress : String?,
    @SerialName("cancellation_note") val cancellationNote: String?,
    @SerialName("cancellation_reason_description") val cancellationReasonDescription : String?,
    @SerialName("cancellation_url") val cancellationUrl : String?,
    @SerialName("cancelled_at") val cancelledAt : Instant?,
    @SerialName("created_at") val createdAt : Instant?,
    @SerialName("email_reminder_sent") val emailReminderSent: Boolean,
    val notes: String?,
    val patient: LinkField,
    @SerialName("patient_case") val patientCase : LinkField? = null,
    @SerialName("updated_at") val updatedAt : Instant?,
    @SerialName("telehealth_url") val telehealthUrl : String?
) : ClinikoRow

@Serializable
class ClinikoAttendeeMessage(val attendees: List<ClinikoAttendee>)