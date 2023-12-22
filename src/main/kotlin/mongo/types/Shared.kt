package mongo.types

import Diffable
import cliniko.PhoneNumber
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import memberDiff

data class Name (
    val first: String,
    val preferred: String? = null,
    val last: String
) : Diffable
{

    fun getFull(usePreferred : Boolean = true) : String {
        return if (usePreferred and !(preferred.isNullOrBlank()))
            "$preferred $last"
        else
            "$first $last"
    }

    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Name?, new = this)

}

data class Address (
    val line1: String?,
    val line2: String?,
    val line3: String?,
    val postCode: Int?,
    val city: String?,
    val state: String?,
    val country: String?,
) : Diffable {
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Address?, new = this)
}

data class ClinikoTimestamps (
    val created : Instant? = null,
    val modified : Instant? = null,
    val archived : Instant? = null
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as ClinikoTimestamps?, new = this)
}

data class Person (
    val name : Name,
    val dob : LocalDate? = null,
    val email: String? = null,
    val address: Address? = null,
    val gender : String? = null,
    val sex : String? = null,
    val pronouns: Pronouns? = null,
    val phones: Map<String, String>? = null // label -> number
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Person?, new = this)
}

data class Pronouns (
    val they : String, // accusative
    val them: String, // nominative
    val their : String, // predicativePossessive
    val theirs : String, // pronominalPossessive
    val themself: String, // reflexive
) : Diffable
{
    companion object {
        fun fromCliniko(clinikoPronouns: cliniko.sections.Pronouns): Pronouns {
            with(clinikoPronouns) {
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

    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Pronouns?, new = this)
}

data class Event (
    val name: String,
    val time: Instant
) : Diffable
{
    override fun diff(other: Any?) : Map<String, Any?>? = memberDiff(old = other as Event?, new = this)
}

fun phonesFromCliniko(clinikoPhones : List<PhoneNumber>?) : Map<String, String> {
    //map number -> type
    return clinikoPhones?.associate { Pair(it.number, it.phoneType) } ?: emptyMap()
}