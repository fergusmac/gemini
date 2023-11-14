import cliniko.ClinikoPatient
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Patient (
    @BsonId val id: ObjectId,
    val cliniko : ClinikoObject,
    val person: Person,
    val medicare: MedicareCard? = null,
    val ndisNumber: Long? = null,
    val notices : String? = null,
    val marketingSource : String? = null,
    val manualBilling : Boolean? = null,
    val stripeId : String? = null,
    val emergencyContact : Person? = null,
    val billingContact : Person? = null,
    val claimant : Claimant? = null,
    val events : List<Event> = emptyList()
) {
//TODO group leader?

    companion object {

        fun fromCliniko(clinikoPatient: ClinikoPatient) : Patient {
            with (clinikoPatient) {
                return Patient(
                    id = ObjectId(),
                    person = Person(
                        name = Name(
                            first = firstName,
                            preferred = preferredFirstName,
                            last = lastName
                        ),
                        dob = dateOfBirth,
                        address = Address(
                            line1 = address1,
                            line2 = address2,
                            line3 = address3,
                            postCode = postCode?.toIntOrNull(),
                            city = city,
                            state = state,
                            country = country
                        ),
                        gender = genderIdentity,
                        sex = sex,
                        phones = patientPhoneNumbers?.map { PhoneNumber.fromCliniko(it) } ?: emptyList(),
                        pronouns = pronouns?.let { Pronouns.fromCliniko(it) },
                    ),
                    medicare = medicare?.toLongOrNull()?.let {
                        MedicareCard(number = it, irn = medicareReferenceNumber?.toIntOrNull())
                    },
                    cliniko = ClinikoObject(
                        id = id,
                        created = createdAt,
                        modified = updatedAt,
                        archived = archivedAt
                    ),
                    notices = apptNotes,
                    marketingSource = referralSource,
                    ndisNumber = null
                )
            }
        }
    }
}

data class Name (
    val first: String,
    val preferred: String?,
    val last: String
)

data class Address (
    val line1: String?,
    val line2: String?,
    val line3: String?,
    val postCode: Int?,
    val city: String?,
    val state: String?,
    val country: String?,
)

data class MedicareCard (
    val number : Long,
    val irn : Int?
)

data class PhoneNumber (
    val number : String,
    val label : String
) {
    companion object {
        fun fromCliniko(clinikoPhone : cliniko.PhoneNumber) : PhoneNumber {
            return PhoneNumber(number = clinikoPhone.number, label = clinikoPhone.phoneType)
        }
    }
}

data class Claimant(
    val person : Person,
    val medicare: MedicareCard
)

data class Pronouns (
    val they : String, // accusative
    val them: String, // nominative
    val their : String, // predicativePossessive
    val theirs : String, // pronominalPossessive
    val themself: String, // reflexive
) {
    companion object {
        fun fromCliniko(clinikoPronouns : cliniko.Pronouns) : Pronouns{
            with (clinikoPronouns) {
                return Pronouns(
                    they = accusative,
                    them = nominative,
                    their = predicativePossessive,
                    theirs = pronominalPossessive,
                    themself = reflexive
                )
            }
        }
    }
}

data class ClinikoObject (
    val id : Long,
    val created : Instant,
    val modified : Instant,
    val archived : Instant?
)

data class Person (
    val name : Name,
    val dob : LocalDate?,
    val address: Address,
    val gender : String?,
    val sex : String?,
    val pronouns: Pronouns?,
    val phones: List<PhoneNumber>
)

data class Event (
    val name: String,
    val time: Instant
)