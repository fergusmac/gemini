package mongo

import com.mongodb.MongoException
import com.mongodb.kotlin.client.coroutine.ClientSession
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

private val logger = KotlinLogging.logger {}

//By default, instant gets written to Mongo as a String - this lets us write it as a Date instead
class InstantCodec : Codec<Instant> {
    override fun encode(writer: BsonWriter, value: Instant?, encoderContext: EncoderContext?) {
        if(value == null)
            writer.writeNull()
        else
            writer.writeDateTime(value.toEpochMilliseconds())
    }

    override fun getEncoderClass(): Class<Instant> {
        return Instant::class.java
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext?): Instant? {
        return if (reader.currentBsonType == BsonType.NULL) null else Instant.fromEpochMilliseconds(reader.readDateTime())
    }

}

//generic codec to store an enum as a string
class EnumCodec<T : Enum<T>> (private val clazz : Class<T>) : Codec<T> {
    override fun encode(writer: BsonWriter, value: T?, encoderContext: EncoderContext?) {
        if(value == null)
            writer.writeNull()
        else
            writer.writeString(value.name)
    }

    override fun getEncoderClass(): Class<T> {
        return clazz
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext?): T? {
        return if (reader.currentBsonType == BsonType.NULL) null
        else
        {
            val str = reader.readString()
            clazz.enumConstants?.find { it.name == str }
        }
    }

    companion object {
        //need to call this inline builder function to instantiate, as we have to pass in a reified T
        inline fun <reified T: Enum<T>> buildCodec() = EnumCodec(T::class.java)
    }
}

suspend fun ClientSession.transact(func : suspend (ClientSession) -> Unit) {
    startTransaction()
    try {
        logger.debug { "Transaction started" }
        func(this)
        //if abort or commit are called within the func block, this will be skipped
        if (hasActiveTransaction()) commitTransaction()
        logger.debug { "Transaction finished" }
    }
    catch (e: MongoException) {
        logger.error { "Transaction aborted due to error: $e" }
        abortTransaction()
    }
}

suspend fun MongoClient.transact(func : suspend (ClientSession) -> Unit) {
    //this closes the session on completion
    startSession().use { it.transact(func) }
}