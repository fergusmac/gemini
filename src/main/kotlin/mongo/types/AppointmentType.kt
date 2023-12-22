package mongo.types

import cliniko.sections.ClinikoApptType
import memberDiff
import org.bson.codecs.pojo.annotations.BsonId

data class AppointmentType (
    @BsonId override val id: Long,
    val name : String,
    val isTelehealth : Boolean,
) : MongoRow {
    override fun diff(other: Any?): Map<String, Any?>? = memberDiff<AppointmentType>(old = other as AppointmentType?, new = this, skip=setOf("id"))

    companion object {
        fun fromCliniko(clinikoApptType : ClinikoApptType, existing : AppointmentType?) : AppointmentType {
            with (clinikoApptType) {
                return AppointmentType(
                    id = id,
                    name = name,
                    isTelehealth = telehealthEnabled
                )
            }
        }
    }
}