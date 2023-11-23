import cliniko.sections.ClinikoPatient
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId


data class Patient (
    @BsonId val id: ObjectId,
    val label : String,
    val cliniko : ClinikoObject,
    val person: Person,
    val medicare: MedicareCard? = null,
    val ndisNumber: Long? = null,
    val marketingSource : String? = null,
    val manualBilling : Boolean? = null,
    val stripeId : String? = null,
    val emergencyContact : Person? = null,
    val billingContact : Person? = null,
    val claimant : Claimant? = null,
    //TODO extra contact
    //TODO events
) : Diffable
{
//TODO group leader?

    companion object {

        fun fromCliniko(clinikoPatient: ClinikoPatient, existing : Patient? = null) : Patient {
            //create or update, keeping any non-cliniko fields the same
            with (clinikoPatient) {
                val name = Name(
                    first = firstName,
                    preferred = preferredFirstName,
                    last = lastName
                )

                return Patient(
                    id = existing?.id ?: ObjectId(),
                    label = name.getFull(),
                    person = Person(
                        name = name,
                        dob = dateOfBirth,
                        email = email,
                        address = Address(
                            line1 = address1,
                            line2 = address2,
                            line3 = address3,
                            postCode = postCode?.toIntOrNull(),
                            city = city,
                            state = state,
                            country = country
                        ),
                        gender = genderIdentity?.nullIfBlank(),
                        sex = sex?.nullIfBlank(),
                        phones = patientPhoneNumbers?.associate { Pair(it.number, it.phoneType) } ?: emptyMap(),
                        pronouns = pronouns?.let { Pronouns.fromCliniko(it) },
                    ),
                    medicare = MedicareCard.fromCliniko(medicare, medicareReferenceNumber),
                    cliniko = ClinikoObject(
                        id = id,
                        created = createdAt,
                        modified = updatedAt,
                        archived = archivedAt
                    ),
                    marketingSource = referralSource,
                    ndisNumber = existing?.ndisNumber,
                    manualBilling = existing?.manualBilling,
                    stripeId = existing?.stripeId,
                    emergencyContact = existing?.emergencyContact,
                    billingContact = existing?.billingContact,
                    claimant = existing?.claimant,
                    // TODO events = existing?.events ?: emptyList()
                )
            }
        }
    }

    override fun diff(other: Any?): Map<String, Any?>? = memberDiff(old = other as Patient?, new = this)
}

data class Name (
    val first: String,
    val preferred: String?,
    val last: String
) : Diffable
{

    fun getFull(usePreferred : Boolean = true) : String {
        return if (usePreferred and !(preferred.isNullOrBlank()))
            "$preferred $last"
        else
            "$first $last"
    }

    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old=other as Name?, new=this)

}


data class Address (
    val line1: String?,
    val line2: String?,
    val line3: String?,
    val postCode: Int?,
    val city: String?,
    val state: String?,
    val country: String?,
) : Diffable {
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old=other as Address?, new=this)
}


data class MedicareCard (
    val number : Long,
    val irn : Int?
) : Diffable
{

    companion object {
        fun fromCliniko(number: String?, irn: String?) : MedicareCard? {
            val num = number?.toLongOrNull() ?: return null
            return MedicareCard(number=num, irn = irn?.toIntOrNull())
        }
    }

    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old=other as MedicareCard?, new=this)
}

data class Claimant (
    val person : Person,
    val medicare: MedicareCard
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old=other as Claimant?, new=this)
}

data class Pronouns (
    val they : String, // accusative
    val them: String, // nominative
    val their : String, // predicativePossessive
    val theirs : String, // pronominalPossessive
    val themself: String, // reflexive
) : Diffable
{
    companion object {
        fun fromCliniko(clinikoPronouns: cliniko.sections.Pronouns): Pronouns {
            with(clinikoPronouns) {
                return Pronouns(
                    they = nominative,
                    them = accusative,
                    their = predicativePossessive,
                    theirs = pronominalPossessive,
                    themself = reflexive
                )
            }
        }
    }

    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old=other as Pronouns?, new=this)
}

data class ClinikoObject (
    val id : Long,
    val created : Instant,
    val modified : Instant,
    val archived : Instant?
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old=other as ClinikoObject?, new=this)
}

data class Person (
    val name : Name,
    val dob : LocalDate?,
    val email: String?,
    val address: Address,
    val gender : String?,
    val sex : String?,
    val pronouns: Pronouns?,
    val phones: Map<String, String>? // label -> number
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old=other as Person?, new=this)
}

data class Event (
    val name: String,
    val time: Instant
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old=other as Event?, new=this)
}