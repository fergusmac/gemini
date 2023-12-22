package mongo.types

import Diffable
import cliniko.sections.*
import memberDiff
import nullIfBlank
import org.bson.codecs.pojo.annotations.BsonId
import copyAndUpsert


interface MongoRow : Diffable {
    val id : Long
}

data class Patient (
    @BsonId override val id: Long,
    val label : String,
    val cliniko : ClinikoTimestamps,
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
                    cliniko = ClinikoTimestamps(
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
    }

    fun copyCombineAppt(clinikoAppt: ClinikoAppointment) : Patient {
        // return a copy with the appt added/updated
        val updatedAppts = appointments.copyAndUpsert(
            filtr = { it.id == clinikoAppt.id },
            upsertFunc = {
                Appointment.fromCliniko(
                    clinikoAppt = clinikoAppt,
                    existing = it
                )
            }
        )

        return copy(appointments = updatedAppts)
    }

    fun copyCombineCase(clinikoCase: ClinikoCase) : Patient {
        // return a copy with the case added/updated
        val updatedRefferals = referrals.copyAndUpsert(
            filtr = { it.id == clinikoCase.id },
            upsertFunc = {
                Referral.fromCliniko(
                    clinikoCase = clinikoCase,
                    existing = it
                )
            }
        )

        return copy(referrals = updatedRefferals)
    }

    fun copyCombineAttendee(clinikoAttendee: ClinikoAttendee) : Patient {
        // return a copy with the matching appt updated with the attendee
        // if appointment hasnt been seen yet, return patient unchanged and try again later

        val bookingId = clinikoAttendee.booking.links.toId()!! // checked not null in caller
        var wasChanged = false

        val updatedAppts = appointments.copyAndUpsert(
            filtr = { it.id == bookingId },
            upsertFunc = {
                wasChanged = true
                it!!.copyCombineAttendee(attendee = clinikoAttendee)
            },
            requireExisting = true
        )

        return if (wasChanged) copy(appointments = updatedAppts) else this
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

data class BillingInfo(
    val customerId : String?,
    val billingContact : Person?,
    val manualBilling: Boolean
)


