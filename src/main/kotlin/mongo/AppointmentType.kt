package mongo

import Diffable
import cliniko.sections.ClinikoApptType
import memberDiff
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class AppointmentType (
    @BsonId override val id: ObjectId,
    val cliniko : ClinikoObject,
    val name : String,
    val isTelehealth : Boolean,
) : MongoRow {
    override fun diff(other: Any?): Map<String, Any?>? = memberDiff<AppointmentType>(old = other as AppointmentType?, new = this, skip=setOf("id"))

    companion object {
        fun fromCliniko(clinikoApptType : ClinikoApptType, existing : AppointmentType?) : AppointmentType {
            with (clinikoApptType) {
                return AppointmentType(
                    id = existing?.id ?: ObjectId(),
                    cliniko = ClinikoObject(id = id),
                    name = name,
                    isTelehealth = telehealthEnabled
                )
            }
        }
    }
}