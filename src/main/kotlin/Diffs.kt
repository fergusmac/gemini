import kotlinx.serialization.descriptors.PrimitiveKind
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties


/**
 * Returns a map of the member properties (by name) that have changed and their new values
 *  If existing is null, return all properties
 */
inline fun <reified T : Any> simpleDiff(old : T?, new : T, skipFields: List<String> = emptyList()) : MutableMap<String, Any?> {
    val results = mutableMapOf<String, Any?>()
    for (prop in T::class.declaredMemberProperties) {
        if (prop.name in skipFields) continue
        val newValue = prop.get(new)
        val oldValue = old?.let(prop)

        if(oldValue == newValue) continue

        results[prop.name] = newValue
    }

    return results
}

// like simpleDiff but for maps - return all KVs that have been added, modified or deleted (signified by K = null)
// this only works for primitive types of V
fun <K, V> simpleMapDiff(old : Map<K, V>?, new : Map<K, V>?) : Map<K, V?> {


    val results = mutableMapOf<K, V?>()
    if (old.isNullOrEmpty()) return new ?: emptyMap()

    new?.forEach {
        // create or modify operation
        if (old.getOrDefault(it.key, null) != it.value) {
            results[it.key] = it.value
        }
    }

    old.forEach {
        // delete operation
        if (new?.contains(it.key) != true) {
            results[it.key] = null
        }
    }

    return results
}


/**
 * As simpleDiff, but recursive. The point is to replace individual fields on sub-objects, rather than the whole object
 * The interface allows each level of the recursion to be e.g. a call to simpleDiff or something else
 */
interface Diffable<T> {
    //can't define a default implementation here, because the compiler can't infer the type of 'this'
    //we would need to pass in the new T as an argument, which is clunky
    fun diff(existing: T?) : Map<String, Any?>
}

// KProperty1<S, T?> means - a property on an object of type S, which is of the type T?
inline fun <reified S, reified T : Diffable<T>> nestedDiff(old: S?, new: S, prop: KProperty1<S, T?>) : Map<String, Any?> {
    val oldValue = old?.let(prop)

    // if both new and old are null, return nothing. If new is null but old isnt, return propName -> null
    val newValue = prop.get(new) ?: return if (oldValue == null) emptyMap() else mapOf(prop.name to null)

    // else recurse. Add the property name to the front of each returned result (so we get a label of A.B.C)
    return newValue.diff(oldValue).mapKeys { "${prop.name}.${it.key}" }
}


// like simpleMapDiff, but uses the Diffable interface. Use if V is an object
fun <K, V : Diffable<V>> nestedMapDiff(old : Map<K, V>?, new : Map<K, V>) : Map<K, Map<String, Any?>?> {

    //for each key K, list of changes to value V's properties, by name. inner null = property on object deleted, outer null = object deleted
    val results = mutableMapOf<K, Map<String, Any?>?>()

    new.forEach {
        val oldValue = old?.getOrDefault(it.key, null)
        if (oldValue != it.value) {
            results[it.key] = it.value.diff(oldValue)
        }
    }

    old?.forEach {
        // delete operation
        if (it.key !in new) {
            results[it.key] = null
        }
    }

    return results
}