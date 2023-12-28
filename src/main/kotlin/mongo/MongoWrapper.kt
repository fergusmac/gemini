package mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import java.lang.IllegalStateException
import kotlinx.coroutines.flow.toList
import mongo.types.*
import org.bson.Document
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
    val metadata = db.getCollection<MongoMetadata>(collectionName = "metadata" )

    suspend fun <T : Any> upsertOne(collection : MongoCollection<T>, id : Long, updatesMap: Map<String, Any?>) {
        val updatesBson = Updates.combine(
            updatesMap.map {
                if (it.value == null) {
                    Updates.unset(it.key)
                } else {
                    Updates.set(it.key, it.value)
                }
            }
        )

        println(updatesBson)

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

    suspend fun <T : Any> getSingleton(collection: MongoCollection<T>) : T? {
        val lst = collection.find().toList()
        return if (lst.isEmpty()) null
        else if (lst.size == 1) lst[0]
        else throw IllegalStateException("Multiple instances of singleton mongo document")
    }

    suspend fun <T: Any> upsertSingleton(collection: MongoCollection<T>, singleton: T) {
        collection.replaceOne(
            filter = Filters.eq(Document()), // match any
            replacement = singleton,
            options = ReplaceOptions().upsert(true)
        )
    }
}