package mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.bson.codecs.configuration.CodecRegistries
import org.bson.types.ObjectId


class MongoWrapper(connectionString : ConnectionString, databaseName: String)
{
    val client = MongoClient.create(connectionString=connectionString)

    val codecRegistry = CodecRegistries.fromRegistries(
        CodecRegistries.fromCodecs(
            InstantCodec(),
            EnumCodec.buildCodec<CancellationType>(),
            EnumCodec.buildCodec<PractitionerKind>()
        ),
        MongoClientSettings.getDefaultCodecRegistry()
    )

    val db = client.getDatabase(databaseName = databaseName).withCodecRegistry(codecRegistry)

    val patients = db.getCollection<Patient>(collectionName = "patients")
    val practs = db.getCollection<Practitioner>(collectionName = "practs")
    val apptTypes = db.getCollection<AppointmentType>(collectionName = "apptTypes")


    suspend fun <T : Any> upsertOne(collection : MongoCollection<T>, id : ObjectId, updatesMap: Map<String, Any?>) {
        val updatesBson = Updates.combine(
            updatesMap.map {
                if (it.value == null) {
                    Updates.unset(it.key)
                } else {
                    Updates.set(it.key, it.value)
                }
            }
        )

        println(updatesBson) //TODO

        collection.updateOne(
            filter = Filters.eq("_id", id),
            update = updatesBson,
            options = UpdateOptions().upsert(true)
        )
    }

    suspend fun <T : Any> removeArrayNulls(collection: MongoCollection<T>, id : ObjectId, arrayFields : List<String>) {
        val updatesBson = Updates.combine(
            arrayFields.map { Updates.pull(it, null) }
        )

        collection.updateOne(
            filter = Filters.eq("_id", id),
            update = updatesBson
        )
    }

    suspend fun <T : Any> getAll(collection: MongoCollection<T>) : List<T> {
        return collection.find().toList()
    }
}