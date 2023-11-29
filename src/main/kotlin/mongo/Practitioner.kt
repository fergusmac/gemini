package mongo

import Diffable
import cliniko.sections.ClinikoPractitioner
import memberDiff
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Practitioner (
    @BsonId val id: ObjectId,
    val label : String,
    val cliniko : ClinikoObject,
    val person : Person,
    val isActive : Boolean,
    var providerNumber : String?,
    var abn : String?,
    val houseFeePercent: Int?,
    val stripeAccount : String?,
    val xeroContactId : String?,
    val isTakingIntakes : Boolean,
    val lettersFolderId : String?,
    val letterTemplates : Map<String, String>? //letter type -> template gdocs id
) : Diffable
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
                    id = existing?.id ?: ObjectId(),
                    label = name.getFull(),
                    cliniko = ClinikoObject(
                        id = id,
                        created = createdAt,
                        modified = updatedAt,
                    ),
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
                    letterTemplates = existing?.letterTemplates
                )
            }
        }
    }

    override fun diff(other: Any?): Map<String, Any?>? = memberDiff(old = other as Practitioner?, new = this)
}