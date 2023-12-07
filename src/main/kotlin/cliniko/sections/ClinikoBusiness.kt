package cliniko.sections

import cliniko.ClinikoRow
import cliniko.LinkField
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SECTION_BUSINESSES = "businesses"

@Serializable
class ClinikoBusiness (
    override val id : Long,
    @SerialName("archived_at") val archivedAt : Instant?,
    @SerialName("address_1") val address1 : String?,
    @SerialName("address_2") val address2 : String?,
    @SerialName("business_name") val businessName : String?,
    val city: String?,
    @SerialName("contact_information") val contactInformation : String?,
    val country: String?,
    @SerialName("post_code") val postCode : String?,
    val state: String?,
    @SerialName("website_address") val websiteAddress : String?
) : ClinikoRow

@Serializable
class ClinikoBusinessMessage(val businesses: List<ClinikoBusiness>)