package mongo.types

import cliniko.sections.ClinikoPractNumber
import cliniko.sections.ClinikoPractitioner
import cliniko.sections.ClinikoUser
import memberDiff
import org.bson.codecs.pojo.annotations.BsonId

data class Practitioner (
    @BsonId override val id: Long,
    val label : String,
    val kind : PractitionerKind?,
    val cliniko : ClinikoTimestamps?,
    val user : User?,
    val person : Person,
    val isActive : Boolean,
    var providerNumber : PractitionerNumber?,
    var abn : PractitionerNumber?,
    val houseFeePercent: Int?,
    val stripeAccount : String?,
    val nextServiceFeeId : Int?,
    val standardPriceDollars : Int?,
    val xeroContactId : String?,
    val isTakingIntakes : Boolean?,
    val lettersFolderId : String?,
    val letterTemplates : Map<String, String>? //letter type -> template gdocs id
) : MongoRow
{

    companion object {

        fun fromCliniko(clinikoPract: ClinikoPractitioner, existing : Practitioner? = null) : Practitioner {
            //create or update, keeping any non-cliniko fields the same
            with (clinikoPract) {
                val name = Name(
                    first = firstName ?: "",
                    last = lastName ?: ""
                )

                return Practitioner(
                    id = id,
                    label = name.getFull(),
                    kind = PractitionerKind.fromString(designation),
                    cliniko = ClinikoTimestamps(
                        created = createdAt,
                        modified = updatedAt,
                    ),
                    //if user not already linked, grab the user id from the pract
                    user = existing?.user ?: user.links.toId()?.let {
                        User(id = it)
                    },
                    person = Person(
                        name = name,
                        email = existing?.person?.email,
                        phones = existing?.person?.phones,
                    ),
                    isActive = clinikoPract.active == true,
                    providerNumber = existing?.providerNumber,
                    abn = existing?.abn,
                    houseFeePercent = existing?.houseFeePercent,
                    stripeAccount = existing?.stripeAccount,
                    xeroContactId = existing?.xeroContactId,
                    isTakingIntakes = existing?.isTakingIntakes ?: false,
                    lettersFolderId = existing?.lettersFolderId,
                    letterTemplates = existing?.letterTemplates,
                    nextServiceFeeId = existing?.nextServiceFeeId,
                    standardPriceDollars = existing?.standardPriceDollars
                )
            }
        }

        fun combineUser(clinikoUser: ClinikoUser, pract : Practitioner) : Practitioner {
            // return a copy with the user updated
            with (clinikoUser) {
                return pract.copy(
                    user = User(
                        id = id,
                        cliniko = ClinikoTimestamps(
                            created = createdAt,
                            modified = updatedAt
                        )
                    ),
                    person = pract.person.copy(
                        email = email,
                        phones = phonesFromCliniko(phoneNumbers)
                        ),
                )
            }

        }

        fun combineRefNumber(clinikoNumber : ClinikoPractNumber, pract : Practitioner) : Practitioner {
            // return a copy with the reference number updated
            // TODO handle multiple PRNs

            with (clinikoNumber) {
                if (referenceNumber == null) return pract

                val practNumber = PractitionerNumber(
                    id = referenceNumber!!,
                    description = name ?: "",
                    cliniko = ClinikoTimestamps(
                        created = createdAt,
                        modified = updatedAt
                    )
                )

                return if (name == "Provider #") {
                    pract.copy(providerNumber = practNumber)
                }
                else if (name == "ABN") {
                    pract.copy(abn = practNumber)
                }
                else {
                    //ignore this number
                    pract
                }

            }
        }
    }

    //skip id as it will already be set when inserted into mongo and including it again will duplicate it
    override fun diff(other: Any?): Map<String, Any?>? = memberDiff(old = other as Practitioner?, new = this, skip=setOf("id"))
}


data class PractitionerNumber (
    val id : String,
    val description: String,
    val cliniko : ClinikoTimestamps
)

data class User (
    val id : Long,
    val cliniko : ClinikoTimestamps? = null
)


enum class PractitionerKind {
    Psychologist, ClinicalPsychologist, Counsellor;

    companion object {
        fun fromString(str : String?) : PractitionerKind? {
            return when (str?.lowercase()?.replace(" ", "")) {
                Psychologist.name.lowercase() -> Psychologist
                ClinicalPsychologist.name.lowercase() -> ClinicalPsychologist
                Counsellor.name.lowercase() -> Counsellor
                else -> null
            }
        }
    }
}