package cliniko.sections

import cliniko.LinkField
import cliniko.PhoneNumber
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


const val SECTION_PRACT_NUMBERS = "practitioner_reference_numbers"

@Serializable
data class ClinikoPractNumber(
    val id : Long,
    val business : LinkField,
    @SerialName("created_at") val createdAt : Instant,
    val name: String?,
    val practitioner : LinkField,
    @SerialName("reference_number") val referenceNumber : String?,
    @SerialName("updated_at") val updatedAt : Instant
)

@Serializable
data class ClinikoPractNumMessage(val numbers: List<ClinikoPractNumber>)