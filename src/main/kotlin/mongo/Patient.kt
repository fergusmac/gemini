package mongo

import Diffable
import ListDiffable
import cliniko.sections.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import memberDiff
import nullIfBlank
import org.bson.codecs.pojo.annotations.BsonId
import printInstantSydney
import upsertElement
import java.sql.Ref


interface MongoRow : Diffable {
    val id : Long
}

data class Patient (
    @BsonId override val id: Long,
    val label : String,
    val cliniko : ClinikoObject,
    val person: Person,
    val medicare: MedicareCard? = null,
    val ndisNumber: Long? = null,
    val marketingSource : String? = null,
    val emergencyContact : Person? = null,
    val claimant : Claimant? = null,
    val billingInfo: BillingInfo? = null,
    val referrals : List<Referral>? = emptyList(),
    val appointments : List<Appointment>? = emptyList()
) : MongoRow
{

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
                    id = id,
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
                        phones = phonesFromCliniko(patientPhoneNumbers),
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
                    emergencyContact = existing?.emergencyContact,
                    billingInfo = existing?.billingInfo,
                    claimant = existing?.claimant,
                    referrals = existing?.referrals,
                    appointments = existing?.appointments
                )
            }
        }

        fun combineCase(clinikoCase: ClinikoCase, patient : Patient) : Patient {
            // return a copy with the case added/updated
            val updatedRefferals = patient.referrals.upsertElement(
                filtr = { it.cliniko.id == clinikoCase.id },
                upsertFunc = {
                    Referral.fromCliniko(
                        clinikoCase = clinikoCase,
                        existing = it)
                }
            )

            return patient.copy(referrals = updatedRefferals)
        }

        fun combineAppt(clinikoAppt: ClinikoAppointment, patient: Patient) : Patient {
            // return a copy with the appt added/updated
            val updatedAppts = patient.appointments.upsertElement(
                filtr = { it.cliniko.id == clinikoAppt.id },
                upsertFunc = {
                    Appointment.fromCliniko(
                        clinikoAppt = clinikoAppt,
                        existing = it)
                }
            )

            return patient.copy(appointments = updatedAppts)
        }

        fun combineAttendee(clinikoAttendee: ClinikoAttendee, patient: Patient) : Patient {
            // return a copy with the matching appt updated with the attendee
            // if appointment hasnt been seen yet, return patient unchanged and try again later

            val bookingId = clinikoAttendee.booking.links.toId()!! // checked not null in caller
            var wasChanged = false

            val updatedAppts = patient.appointments.upsertElement(
                filtr = { it.cliniko.id == bookingId },
                upsertFunc = {
                    wasChanged = true
                    Appointment.combineAttendee(
                        attendee = clinikoAttendee,
                        existing = it!!
                    )
                },
                requireExisting = true
            )

            return if (wasChanged) patient.copy(appointments = updatedAppts) else patient
        }
    }

    //skip id as it will already be set when inserted into mongo and including it again will duplicate it
    override fun diff(other: Any?): Map<String, Any?>? = memberDiff(old = other as Patient?, new = this, skip=setOf("id"))
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

    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as MedicareCard?, new = this)
}

data class Claimant (
    val person : Person,
    val medicare: MedicareCard?
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Claimant?, new = this)
}


//TODO add a human readable label
data class Referral (
    val cliniko : ClinikoObject,
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
                    cliniko = ClinikoObject(
                        id = id,
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

    override fun getDiffKey(): String = cliniko.id.toString()

    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Referral?, new = this)
}


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

        fun combineAttendee(attendee: ClinikoAttendee, existing : Appointment) : Appointment {
            //all the other fields are doubled up on the individual appointment, so ignore everything else
            return existing.copy(
                cancellationUrl = attendee.cancellationUrl,
                patientTelehealthUrl = attendee.telehealthUrl)
        }
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


data class BillingInfo(
    val customerId : String?,
    val billingContact : Person?,
    val manualBilling: Boolean
)


