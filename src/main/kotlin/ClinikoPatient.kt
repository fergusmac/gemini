import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ClinikoPatient(@SerialName("first_name") val firstName : String)


@Serializable
data class ClinikoPatientMessage(val patients: List<ClinikoPatient>)