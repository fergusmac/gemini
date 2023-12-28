package mongo.types

import kotlinx.datetime.Instant
import org.bson.codecs.pojo.annotations.BsonId

data class MongoMetadata(
    val lastClinikoUpdate : Instant? = null
)