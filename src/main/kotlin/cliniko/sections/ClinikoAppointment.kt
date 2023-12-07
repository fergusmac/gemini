package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SECTION_APPOINTMENTS = "individual_appointments"

@Serializable
data class ClinikoAppointment(
    //individual appt
    override val id : Long,
    @SerialName("archived_at") val archivedAt : Instant?,
    val business : LinkField,
    @SerialName("booking_ip_address") val bookingIpAddress : String?,
    @SerialName("cancellation_note") val cancellationNote: String?,
    @SerialName("cancellation_reason_description") val cancellationReasonDescription : String?,
    @SerialName("cancelled_at") val cancelledAt : Instant?,
    @SerialName("created_at") val createdAt : Instant?,
    @SerialName("did_not_arrive") val didNotArrive : Boolean?,
    @SerialName("email_reminder_sent") val emailReminderSent: Boolean,
    @SerialName("ends_at") val endsAt: Instant,
    val notes: String?,
    val patient: LinkField,
    @SerialName("patient_arrived") val patientArrived: Boolean,
    @SerialName("patient_case") val patientCase : LinkField? = null,
    val practitioner: LinkField,
    @SerialName("starts_at") val startsAt: Instant,
    @SerialName("updated_at") val updatedAt : Instant?,
    @SerialName("telehealth_url") val practTelehealthUrl : String?
) : ClinikoRow

@Serializable
data class ClinikoAppointmentMessage(@SerialName(SECTION_APPOINTMENTS) val appointments: List<ClinikoAppointment>)

