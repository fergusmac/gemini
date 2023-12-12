package mongo

import Diffable
import cliniko.sections.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import memberDiff
import nullIfBlank
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId


interface MongoRow : Diffable {
    val id : ObjectId
}

data class Patient (
    @BsonId override val id: ObjectId,
    val label : String,
    val cliniko : ClinikoObject,
    val person: Person,
    val medicare: MedicareCard? = null,
    val ndisNumber: Long? = null,
    val marketingSource : String? = null,
    val emergencyContact : Person? = null,
    val claimant : Claimant? = null,
    val billingInfo: BillingInfo? = null,
    val referrals : Map<String, Referral>? = emptyMap(), //clinikoId (as string) -> Referral
    val appointments : Map<String, Appointment>? = emptyMap() // clinikoId string -> Appt. We have to use strings as the key for maps in Mongo
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
            with (clinikoCase) {
                val existingReferral = patient.referrals?.getOrDefault(id.toString(), null)
                val allReferrals = patient.referrals?.toMutableMap() ?: mutableMapOf()

                val newReferral = Referral.fromCliniko(clinikoCase = this, existing = existingReferral)
                allReferrals[newReferral.cliniko.id.toString()] = newReferral

                return patient.copy(referrals = allReferrals)
            }
        }

        fun combineAppt(clinikoAppt: ClinikoAppointment, patient: Patient) : Patient {
            // return a copy with the appt added/updated
            with (clinikoAppt) {
                val existingAppt = patient.appointments?.getOrDefault(id.toString(), null)
                val allAppointments = patient.appointments?.toMutableMap() ?: mutableMapOf()

                val newAppt = Appointment.fromCliniko(clinikoAppt = clinikoAppt, existing = existingAppt)
                allAppointments[newAppt.cliniko.id.toString()] = newAppt

                return patient.copy(appointments = allAppointments)
            }
        }

        fun combineAttendee(clinikoAttendee: ClinikoAttendee, patient: Patient) : Patient {
            // return a copy with the matching appt updated with the attendee
            with (clinikoAttendee) {

                val bookingId = booking.links.toId()!! // checked not null in caller

                // if appointment hasnt been seen yet, return patient unchanged and try again later
                val existingAppt = patient.appointments?.getOrDefault(bookingId.toString(), null) ?: return patient

                val allAppointments = patient.appointments.toMutableMap()

                val newAppt = Appointment.combineAttendee(attendee = clinikoAttendee, existing = existingAppt)
                allAppointments[newAppt.cliniko.id.toString()] = newAppt

                return patient.copy(appointments = allAppointments)
            }
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


data class Referral (
    val id: ObjectId,
    val cliniko : ClinikoObject,
    val name : String,
    val referralDate : LocalDate?,
    val expiryDate : LocalDate?,
    val maxAppointments : Int?,
    val closedInCliniko : Boolean,
    val clinikoContactId : Long?,
) : Diffable {

    companion object {
        fun fromCliniko(clinikoCase : ClinikoCase, existing : Referral?) : Referral {
            with (clinikoCase) {
                return Referral(
                    id = existing?.id ?: ObjectId(),
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
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Referral?, new = this)
}


data class Appointment (
    val id: ObjectId,
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
    val dateClaimed : LocalDate?

) : Diffable {

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
                    id = existing?.id ?: ObjectId(),
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
                    dateClaimed = existing?.dateClaimed
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