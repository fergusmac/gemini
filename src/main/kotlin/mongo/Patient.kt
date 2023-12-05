package mongo

import Diffable
import cliniko.sections.ClinikoAppointment
import cliniko.sections.ClinikoCase
import cliniko.sections.ClinikoPatient
import cliniko.sections.ClinikoUser
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import memberDiff
import nullIfBlank
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
    val referrals : Map<String, Referral>? = emptyMap(), //clinikoId (as string) -> Referral
    val appointments : Map<String, Appointment>? = emptyMap() // clinikoId string -> Appt
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
                    manualBilling = existing?.manualBilling,
                    stripeId = existing?.stripeId,
                    emergencyContact = existing?.emergencyContact,
                    billingContact = existing?.billingContact,
                    claimant = existing?.claimant,
                    referrals = existing?.referrals,
                    appointments = existing?.appointments
                    // TODO events = existing?.events ?: emptyList()
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
    val medicare: MedicareCard
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Claimant?, new = this)
}


data class Referral (
    val id: ObjectId,
    val cliniko : ClinikoObject,
    val name : String,
    val referralType : String,
    val referralDate : LocalDate?,
    val expiryDate : LocalDate?,
    val maxAppointments : Int?,
    val closedInCliniko : Boolean,
    val status : String,
    //TODO appointment ids?
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
                    referralType = "", //TODO
                    referralDate = issueDate,
                    expiryDate = expiryDate,
                    maxAppointments = maxSessions,
                    closedInCliniko = closed,
                    status = "", //TODO
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
    val telehealthUrl : String?


) : Diffable {

    companion object {

        fun fromCliniko(clinikoAppt: ClinikoAppointment, existing : Appointment?) : Appointment {
            with (clinikoAppt) {
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
                    cancellationUrl = null, //TODO
                    telehealthUrl = telehealthUrl
                )
            }
        }
    }
    override fun diff(other: Any?): Map<String, Any?>? = memberDiff(old = other as Appointment?, new = this)

}

enum class CancellationType {
    Cancellation, NonArrival
}

data class Cancellation (
    val time : Instant?,
    val kind : CancellationType
)