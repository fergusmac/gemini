package cliniko.sections

import cliniko.ClinikoRow
import cliniko.PhoneNumber
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


const val SECTION_CONTACTS = "contacts"

@Serializable
data class ClinikoContact(
    override val id : Long,
    @SerialName("address_1") val address1 : String?,
    @SerialName("address_2") val address2 : String?,
    @SerialName("address_3") val address3 : String?,
    @SerialName("archived_at") val archivedAt : Instant?,
    val city : String?,
    @SerialName("company_name") val companyName : String?,
    val country : String?,
    @SerialName("created_at") val createdAt : Instant,
    val email : String?,
    @SerialName("first_name") val firstName : String?,
    @SerialName("last_name") val lastName : String?,
    @SerialName("phone_numbers") val phoneNumbers: List<PhoneNumber>?,
    @SerialName("post_code") val postCode : String?,
    @SerialName("provider_number") val providerNumber : String?,
    val state : String?,
    @SerialName("updated_at") val updatedAt : Instant,
) : ClinikoRow

@Serializable
data class ClinikoContactMessage(val contacts: List<ClinikoContact>)