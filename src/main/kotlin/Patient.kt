import cliniko.ClinikoPatient
import kotlin.reflect.full.declaredMemberProperties
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

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
    //TODO events
)
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
}

fun Patient?.diff(other : Patient?) : Map<String, Any?>? {

    if (this == other) return emptyMap()

    if (this == null) return null

    // id is handled specially - but it never changes anyway
    val skipProps = mutableSetOf(Patient::id.name)

    val results = mutableMapOf<String, Any?>()
    results.putAllPrefixed(Patient::person.name.also { skipProps.add(it) }, person.diff(other?.person))
    results.putAllPrefixed(Patient::medicare.name.also { skipProps.add(it) }, medicare.diff(other?.medicare))
    results.putAllPrefixed(Patient::cliniko.name.also { skipProps.add(it) }, cliniko.diff(other?.cliniko))
    results.putAllPrefixed(Patient::emergencyContact.name.also { skipProps.add(it) }, emergencyContact.diff(other?.emergencyContact))
    results.putAllPrefixed(Patient::billingContact.name.also { skipProps.add(it) }, billingContact.diff(other?.billingContact))
    results.putAllPrefixed(Patient::claimant.name.also { skipProps.add(it) }, claimant.diff(other?.claimant))

    results.putAll(memberDiff(old = other, new = this, skip = skipProps)!!)

    return results
}


data class Name (
    val first: String,
    val preferred: String?,
    val last: String
)
{

    fun getFull(usePreferred : Boolean = true) : String {
        return if (usePreferred and !(preferred.isNullOrBlank()))
            "$preferred $last"
        else
            "$first $last"
    }

}
fun Name?.diff(other : Name?) : Map<String, Any?>? = memberDiff(old = other, new = this)

data class Address (
    val line1: String?,
    val line2: String?,
    val line3: String?,
    val postCode: Int?,
    val city: String?,
    val state: String?,
    val country: String?,
)
fun Address?.diff(other : Address?) : Map<String, Any?>? = memberDiff(old = other, new = this)


data class MedicareCard (
    val number : Long,
    val irn : Int?
) {

    companion object {
        fun fromCliniko(number: String?, irn: String?) : MedicareCard? {
            val num = number?.toLongOrNull() ?: return null
            return MedicareCard(number=num, irn = irn?.toIntOrNull())
        }
    }
}
fun MedicareCard?.diff(other : MedicareCard?) : Map<String, Any?>? = memberDiff(old = other, new = this)


data class Claimant (
    val person : Person,
    val medicare: MedicareCard
)
fun Claimant?.diff(other : Claimant?) : Map<String, Any?>? {

    if (this == other) return emptyMap()

    if (this == null) return null

    val results = mutableMapOf<String, Any?>()
    results.putAllPrefixed(Claimant::medicare.name, medicare.diff(other?.medicare))
    results.putAllPrefixed(Claimant::person.name, person.diff(other?.person))
    return results
}


data class Pronouns (
    val they : String, // accusative
    val them: String, // nominative
    val their : String, // predicativePossessive
    val theirs : String, // pronominalPossessive
    val themself: String, // reflexive
)
{
    companion object {
        fun fromCliniko(clinikoPronouns : cliniko.Pronouns) : Pronouns{
            with (clinikoPronouns) {
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
}
fun Pronouns?.diff(other : Pronouns?) : Map<String, Any?>? = memberDiff(old = other, new = this)


data class ClinikoObject (
    val id : Long,
    val created : Instant,
    val modified : Instant,
    val archived : Instant?
)
fun ClinikoObject?.diff(other : ClinikoObject?) : Map<String, Any?>? = memberDiff(old = other, new = this)

data class Person (
    val name : Name,
    val dob : LocalDate?,
    val email: String?,
    val address: Address,
    val gender : String?,
    val sex : String?,
    val pronouns: Pronouns?,
    val phones: Map<String, String>? // label -> number
)

fun Person?.diff(other : Person?) : Map<String, Any?>? {

    if (this == other) return emptyMap()

    if (this == null) return null

    val skipProps = mutableSetOf<String>()

    val results = mutableMapOf<String, Any?>()
    results.putAllPrefixed(Person::name.name.also { skipProps.add(it) }, name.diff(other?.name))
    results.putAllPrefixed(Person::address.name.also { skipProps.add(it) }, address.diff(other?.address))
    results.putAllPrefixed(Person::pronouns.name.also { skipProps.add(it) }, pronouns.diff(other?.pronouns))
    results.putAll(mapDiff(name = Person::phones.name.also { skipProps.add(it) }, old = other?.phones, new = this.phones))

    results.putAll(memberDiff(old = other, new = this, skip = skipProps)!!)

    return results
}

data class Event (
    val name: String,
    val time: Instant
)
fun Event?.diff(other : Event?) : Map<String, Any?>? = memberDiff(old = other, new = this)

