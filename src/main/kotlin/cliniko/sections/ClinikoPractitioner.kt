package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


const val SECTION_PRACTITIONERS = "practitioners"

@Serializable
data class ClinikoPractitioner(
    override val id : Long,
    val active: Boolean?,
    @SerialName("created_at") val createdAt : Instant,
    val designation: String?,
    @SerialName("first_name") val firstName : String?,
    @SerialName("last_name") val lastName : String?,
    @SerialName("updated_at") val updatedAt : Instant,
    val user: LinkField
) : ClinikoRow

@Serializable
data class ClinikoPractMessage(val practitioners: List<ClinikoPractitioner>)