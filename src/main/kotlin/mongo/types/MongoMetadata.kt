package mongo.types

import kotlinx.datetime.Instant

data class MongoMetadata(
    val lastClinikoUpdate : Instant? = null
)