package cliniko.sections

import cliniko.ClinikoRow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SECTION_APPT_TYPES = "appointment_types"

@Serializable
class ClinikoApptType (
    override val id: Long,
    val name: String,
    @SerialName("max_attendees") val maxAttendees: Int,
    @SerialName("telehealth_enabled") val telehealthEnabled: Boolean
    ) : ClinikoRow

@Serializable
class ClinikoApptTypeMessage(@SerialName(SECTION_APPT_TYPES) val apptTypes: List<ClinikoApptType>)