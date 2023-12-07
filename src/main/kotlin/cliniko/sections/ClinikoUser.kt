package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import cliniko.PhoneNumber
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


const val SECTION_USERS = "users"

@Serializable
data class ClinikoUser(
    override val id : Long,
    val active: Boolean?,
    @SerialName("created_at") val createdAt : Instant,
    val email: String,
    @SerialName("first_name") val firstName : String,
    @SerialName("last_name") val lastName : String,
    @SerialName("phone_numbers") val phoneNumbers: List<PhoneNumber>?,
    @SerialName("updated_at") val updatedAt : Instant,
    @SerialName("user_active") val userActive : Boolean?,
) : ClinikoRow

@Serializable
data class ClinikoUserMessage(val users: List<ClinikoUser>)