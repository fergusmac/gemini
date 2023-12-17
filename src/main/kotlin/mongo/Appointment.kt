package mongo

import Diffable
import ListDiffable
import cliniko.sections.ClinikoAppointment
import cliniko.sections.ClinikoAttendee
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import memberDiff
import printInstantSydney

data class Appointment (
    val label : String?,
    val cliniko : ClinikoObject,
    val startTime : Instant,
    val endTime : Instant,
    val wasBookedOnline : Boolean,
    val cancellationUrl : String?,
    val patientTelehealthUrl : String?,
    val referralId : Long?,
    val cancellation : Cancellation?,
    val hasArrived : Boolean,
    val wasInvoiced : Boolean?,
    val dateClaimed : LocalDate?,
) : ListDiffable {
    companion object {

        fun fromCliniko(clinikoAppt: ClinikoAppointment, existing : Appointment?) : Appointment {
            with (clinikoAppt) {
                val cancellation = if (cancelledAt != null) {
                    Cancellation(
                        time = cancelledAt,
                        kind = CancellationType.Cancellation,
                        note = cancellationNote,
                        reason = cancellationReasonDescription)
                } else if (didNotArrive == true) {
                    Cancellation(time = null, kind = CancellationType.NonArrival)
                } else {
                    null
                }

                return Appointment(
                    cliniko = ClinikoObject(
                        id = id,
                        created = createdAt,
                        modified = updatedAt,
                        archived = archivedAt,
                    ),
                    startTime = startsAt,
                    endTime = endsAt,
                    wasBookedOnline = !bookingIpAddress.isNullOrBlank(),
                    cancellationUrl = existing?.cancellationUrl,
                    patientTelehealthUrl = existing?.patientTelehealthUrl,
                    referralId = patientCase?.links?.toId(),
                    cancellation = cancellation,
                    hasArrived = patientArrived,
                    wasInvoiced = existing?.wasInvoiced,
                    dateClaimed = existing?.dateClaimed,
                    label = printInstantSydney(startsAt)
                )
            }
        }
    }

    fun copyCombineAttendee(attendee: ClinikoAttendee) : Appointment {
        //all the other fields are doubled up on the individual appointment, so ignore everything else
        return copy(
            cancellationUrl = attendee.cancellationUrl,
            patientTelehealthUrl = attendee.telehealthUrl)
    }

    override fun getDiffKey(): String = cliniko.id.toString()

    override fun diff(other: Any?): Map<String, Any?>? = memberDiff(old = other as Appointment?, new = this)

}

enum class CancellationType {
    Cancellation, NonArrival
}

data class Cancellation (
    val time : Instant?,
    val kind : CancellationType,
    val note : String? = null,
    val reason : String? = null
) : Diffable {

    override fun diff(other: Any?): Map<String, Any?>? = memberDiff(old = other as Cancellation?, new = this)
}
