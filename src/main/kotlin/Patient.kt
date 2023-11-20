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
    val events : List<Event>? = null //TODO make map
) : Diffable<Patient>
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
                        gender = genderIdentity,
                        sex = sex,
                        phones = patientPhoneNumbers?.associate { Pair(it.number, it.phoneType) } ?: emptyMap(),
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
                    marketingSource = referralSource,
                    ndisNumber = existing?.ndisNumber,
                    manualBilling = existing?.manualBilling,
                    stripeId = existing?.stripeId,
                    emergencyContact = existing?.emergencyContact,
                    billingContact = existing?.billingContact,
                    claimant = existing?.claimant,
                    events = existing?.events ?: emptyList()
                )
            }
        }
    }

    override fun diff(existing: Patient?): Map<String, Any?> {

        val results = mutableMapOf<String, Any?>()

        results.putAll(nestedDiff(old = existing, new = this, prop = Patient::person))
        results.putAll(nestedDiff(old = existing, new = this, prop = Patient::medicare))
        results.putAll(nestedDiff(old = existing, new = this, prop = Patient::cliniko))
        results.putAll(nestedDiff(old = existing, new = this, prop = Patient::emergencyContact))
        results.putAll(nestedDiff(old = existing, new = this, prop = Patient::billingContact))
        results.putAll(nestedDiff(old = existing, new = this, prop = Patient::claimant))

        results.putAll(
            simpleDiff(existing, this,
                skipFields = listOf(
                    Patient::person.name,
                    Patient::medicare.name,
                    Patient::cliniko.name,
                    Patient::emergencyContact.name,
                    Patient::billingContact.name,
                    Patient::claimant.name,
                    Patient::events.name,  //TODO
                    Patient::id.name)
            )
        )

        return results
    }
}


data class Name (
    val first: String,
    val preferred: String?,
    val last: String
) : Diffable<Name>
{

    fun getFull(usePreferred : Boolean = true) : String {
        return if (usePreferred and !(preferred.isNullOrBlank()))
            "$preferred $last"
        else
            "$first $last"
    }

    override fun diff(existing: Name?): Map<String, Any?> = simpleDiff(old=existing, new=this)
}

data class Address (
    val line1: String?,
    val line2: String?,
    val line3: String?,
    val postCode: Int?,
    val city: String?,
    val state: String?,
    val country: String?,
) : Diffable<Address> {
    override fun diff(existing: Address?): Map<String, Any?> = simpleDiff(old=existing, new=this)
}


data class MedicareCard (
    val number : Long,
    val irn : Int?
) : Diffable<MedicareCard> {
    override fun diff(existing: MedicareCard?): Map<String, Any?> = simpleDiff(old=existing, new=this)
}

data class Claimant (
    val person : Person,
    val medicare: MedicareCard
) : Diffable<Claimant>
{
    override fun diff(existing: Claimant?): Map<String, Any?> {

        val results = mutableMapOf<String, Any?>()

        results.putAll(nestedDiff(old = existing, new = this, prop = Claimant::person))
        results.putAll(nestedDiff(old = existing, new = this, prop = Claimant::medicare))

        return results
    }
}

data class Pronouns (
    val they : String, // accusative
    val them: String, // nominative
    val their : String, // predicativePossessive
    val theirs : String, // pronominalPossessive
    val themself: String, // reflexive
) : Diffable<Pronouns>
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

    override fun diff(existing: Pronouns?): Map<String, Any?> = simpleDiff(old=existing, new=this)
}

data class ClinikoObject (
    val id : Long,
    val created : Instant,
    val modified : Instant,
    val archived : Instant?
) : Diffable<ClinikoObject>
{

    override fun diff(existing: ClinikoObject?): Map<String, Any?> = simpleDiff(old=existing, new=this)
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
) : Diffable<Person> {

    override fun diff(existing: Person?): Map<String, Any?> {

        val results = mutableMapOf<String, Any?>()

        results.putAll(nestedDiff(old = existing, new = this, prop = Person::name))
        results.putAll(nestedDiff(old = existing, new = this, prop = Person::address))
        results.putAll(nestedDiff(old = existing, new = this, prop = Person::pronouns))

        results.putAll(phones.diff(existing?.phones, prefix=Person::phones.name))

        results.putAll(
            simpleDiff(existing, this,
                skipFields = results.map { it.key.firstDot() }
            )
        )

        return results
    }
}


data class Event (
    val name: String,
    val time: Instant
) : Diffable<Event>
{
    override fun diff(existing: Event?): Map<String, Any?> = simpleDiff(old=existing, new=this)
}


//By default, instant gets written to Mongo as a String - this lets us write it as a Date instead
class InstantCodec : Codec<Instant> {
    override fun encode(writer: BsonWriter, value: Instant?, encoderContext: EncoderContext?) {
        if(value == null)
            writer.writeNull()
        else
            writer.writeDateTime(value.toEpochMilliseconds())
    }

    override fun getEncoderClass(): Class<Instant> {
        return Instant::class.java
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext?): Instant? {
        return if (reader.currentBsonType == BsonType.NULL) null else Instant.fromEpochMilliseconds(reader.readDateTime())
    }

}