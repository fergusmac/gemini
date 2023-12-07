package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SECTION_AVAILABILITY = "daily_availabilities"

@Serializable
class Availability (
    @SerialName("starts_at") val startsAt : LocalTime,
    @SerialName("ends_at") val endsAt : LocalTime
)

@Serializable
class ClinikoAvailability (
    override val id : Long,
    val business: LinkField,
    @SerialName("day_of_week") val dayOfWeek : Int, // 0 = Monday
    val practitioner: LinkField
) : ClinikoRow

@Serializable
class ClinikoAvailabilityMessage(@SerialName(SECTION_AVAILABILITY) val availabilities: List<ClinikoAvailability>)