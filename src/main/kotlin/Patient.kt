import cliniko.ClinikoPatient
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.BsonDateTime
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.codecs.BsonDateTimeCodec
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.kotlinx.BsonEncoder
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonRepresentation
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
    val events : List<Event> = emptyList()
) {
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
}

data class Name (
    val first: String,
    val preferred: String?,
    val last: String
) {

    fun getFull(usePreferred : Boolean = true) : String {
        return if (usePreferred and !(preferred.isNullOrBlank()))
            "$preferred $last"
        else
            "$first $last"
    }
}

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