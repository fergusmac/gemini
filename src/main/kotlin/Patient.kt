import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Patient (
    @BsonId val id: ObjectId,
    val clinikoId : Long,
    val name: Name
)

data class Name (
    val first: String,
    val preferred: String?,
    val last: String
)