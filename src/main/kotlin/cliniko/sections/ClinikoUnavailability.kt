package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import cliniko.PhoneNumber
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


const val SECTION_UNAVAILABILITIES = "unavailable_blocks"

@Serializable
data class ClinikoUnavailability(
    override val id : Long,
    val business: LinkField,
    @SerialName("created_at") val createdAt : Instant,
    @SerialName("deleted_at") val deletedAt : Instant?,
    @SerialName("ends_at") val endsAt : Instant,
    @SerialName("starts_at") val startsAt : Instant,
    val notes: String?,
    val practitioner: LinkField,
    @SerialName("updated_at") val updatedAt : Instant,
) : ClinikoRow

@Serializable
data class ClinikoUnavailabilityMessage(@SerialName(SECTION_UNAVAILABILITIES) val unavailabilities: List<ClinikoUnavailability>)