package cliniko.sections

import cliniko.ClinikoRow
import cliniko.PhoneNumber
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SECTION_PATIENTS = "patients"

@Serializable
data class ClinikoPatient(
    override val id : Long,
    @SerialName("address_1") val address1 : String?,
    @SerialName("address_2") val address2 : String?,
    @SerialName("address_3") val address3 : String?,
    @SerialName("appointment_notes") val apptNotes : String?,
    @SerialName("archived_at") val archivedAt : Instant?,
    val city : String?,
    val country : String?,
    @SerialName("created_at") val createdAt : Instant,
    @SerialName("date_of_birth") val dateOfBirth : LocalDate?,
    val email : String?,
    @SerialName("emergency_contact") val emergencyContact : String?,
    @SerialName("first_name") val firstName : String,
    @SerialName("gender_identity") val genderIdentity : String?,
    @SerialName("invoice_email") val invoiceEmail : String?,
    @SerialName("last_name") val lastName : String,
    val medicare: String?,
    @SerialName("medicare_reference_number") val medicareReferenceNumber : String?,
    val notes: String?,
    @SerialName("patient_phone_numbers") val patientPhoneNumbers: List<PhoneNumber>?,
    @SerialName("post_code") val postCode : String?,
    @SerialName("preferred_first_name") val preferredFirstName : String?,
    val pronouns: Pronouns?,
    @SerialName("referral_source") val referralSource : String?,
    val sex : String?,
    val state : String?,
    @SerialName("updated_at") val updatedAt : Instant,
    ) : ClinikoRow



@Serializable
data class Pronouns(
    val accusative : String, // e.g. they
    val nominative: String, // e.g. them
    @SerialName("predicative_possessive") val predicativePossessive : String, // e.g. their
    @SerialName("pronominal_possessive") val pronominalPossessive : String, // e.g. theirs
    val reflexive: String, // e.g. themself
)

@Serializable
data class ClinikoPatientMessage(val patients: List<ClinikoPatient>)


