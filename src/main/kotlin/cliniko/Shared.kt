package cliniko

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhoneNumber(
    val number : String,
    @SerialName("phone_type") val phoneType : String
)

@Serializable
data class Links (
    val self : String
)

@Serializable
data class LinkField(
    val links : Links
)

//used to get a sneak peek at how many entries there are, regardless of message type
@Serializable
data class ClinikoGenericMessage(
    @SerialName("total_entries") val totalEntries: Int
)