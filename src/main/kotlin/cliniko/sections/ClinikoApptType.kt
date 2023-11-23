package cliniko.sections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SECTION_APPT_TYPES = "appointment_types"

@Serializable
class ClinikoApptType (
    val id: Long,
    val name: String,
    @SerialName("max_attendees") val maxAttendees: Int,
    @SerialName("telehealth_enabled") val telehealthEnabled: Boolean
    )

@Serializable
class ClinikoApptTypeMessage(@SerialName("appointment_types") val apptTypes: List<ClinikoApptType>)