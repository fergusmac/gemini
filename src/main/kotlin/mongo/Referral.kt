package mongo

import ListDiffable
import cliniko.sections.ClinikoCase
import kotlinx.datetime.LocalDate
import memberDiff

//TODO add a human readable label
data class Referral (
    val id : Long,
    val cliniko : ClinikoTimestamps,
    val name : String,
    val referralDate : LocalDate?,
    val expiryDate : LocalDate?,
    val maxAppointments : Int?,
    val closedInCliniko : Boolean,
    val clinikoContactId : Long?,
) : ListDiffable {

    companion object {
        fun fromCliniko(clinikoCase : ClinikoCase, existing : Referral?) : Referral {
            with (clinikoCase) {
                return Referral(
                    id = id,
                    cliniko = ClinikoTimestamps(
                        created = createdAt,
                        modified = updatedAt,
                        archived = archivedAt
                    ),
                    name = name,
                    referralDate = issueDate,
                    expiryDate = expiryDate,
                    maxAppointments = maxSessions,
                    closedInCliniko = closed,
                    clinikoContactId = contact?.links?.toId()
                )
            }
        }
    }

    override fun getDiffKey(): String = id.toString()

    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Referral?, new = this)
}

