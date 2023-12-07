package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import cliniko.PhoneNumber
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


const val SECTION_PRACT_NUMBERS = "practitioner_reference_numbers"

@Serializable
data class ClinikoPractNumber(
    override val id : Long,
    val business : LinkField,
    @SerialName("created_at") val createdAt : Instant,
    val name: String?,
    val practitioner : LinkField,
    @SerialName("reference_number") val referenceNumber : String?,
    @SerialName("updated_at") val updatedAt : Instant
) : ClinikoRow

@Serializable
data class ClinikoPractNumMessage(@SerialName(SECTION_PRACT_NUMBERS) val numbers: List<ClinikoPractNumber>)