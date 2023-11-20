import kotlinx.serialization.descriptors.PrimitiveKind
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties


/**
 * Returns a map of the member properties (by name) that have changed and their new values
 *  If existing is null, return all properties
 *  If we want to 'drill down' to changes within T, need to use nestedDiff
 */
inline fun <reified T : Any> simpleDiff(old : T?, new : T?, skipFields: List<String> = emptyList()) : MutableMap<String, Any?> {
    val results = mutableMapOf<String, Any?>()
    for (prop in T::class.declaredMemberProperties) {
        if (prop.name in skipFields) continue
        val newValue = new?.let(prop)
        val oldValue = old?.let(prop)

        if(oldValue == newValue) continue

        results[prop.name] = newValue
    }

    return results
}

/** like simpleDiff but for maps - return all KVs that have been added, modified or deleted (signified by K = null)
* if there is no change, return empty map
* if we want to 'drill down' to changes inside V, need to use nestedDiff
*/
fun <V> Map<String, V>?.diff(old: Map<String, V>?, prefix: String = "") : Map<String, V?> {

    val results = mutableMapOf<String, V?>()

    if (old.isNullOrEmpty()){
        results.putAll(this ?: emptyMap())
    }
    else {

        this?.forEach {
            // create or modify operation
            if (old.getOrDefault(it.key, null) != it.value) {
                results[it.key] = it.value
            }
        }

        old.forEach {
            // delete operation
            if (this?.contains(it.key) != true) {
                results[it.key] = null
            }
        }

    }

    if (prefix.isNotEmpty()) return results.mapKeys { prefix dot it.key }

    return results
}



interface Diffable<T> {
    //can't define a default implementation here, because the compiler can't infer the type of 'this'
    //we would need to pass in the new obj as an argument, which is clunky
    fun diff(existing: T?) : Map<String, Any?>
}

/**
 * As simpleDiff, but recursive. The point is to replace individual fields on sub-objects, rather than the whole object
 * The interface allows each level of the recursion to be e.g. a call to diff, nestedDiff, or something else
 * KProperty1<S, T?> means - a property on an object of type S, which is of the type T?
 */
inline fun <reified S, reified T : Diffable<T>> nestedDiff(old: S?, new: S?, prop: KProperty1<S, T?>) : Map<String, Any?> {
    val oldValue = old?.let(prop)
    val newValue = new?.let(prop)

    // if both new and old are same (including both null), return nothing. If new is null but old isnt, return propName -> null
    if (oldValue == newValue) return emptyMap()

    if (newValue == null) return mapOf(prop.name to null)

    // else recurse. Add the property name to the front of each returned result (so we get a label of A.B.C)
    return newValue.diff(oldValue).mapKeys { "${prop.name}.${it.key}" }
}


/** Like nestedDiff, but for maps
 */
fun <V : Diffable<V>> Map<String, V>?.nestedDiff(old : Map<String, V>?) : Map<String, Map<String, Any?>?> {

    //for each key K, list of changes to value V's properties, by name. inner null = property on object deleted, outer null = object deleted
    val results = mutableMapOf<String, Map<String, Any?>?>()

    this?.forEach {
        val oldValue = old?.getOrDefault(it.key, null)
        if (oldValue != it.value) {
            results[it.key] = it.value.diff(oldValue)
        }
    }

    old?.forEach {
        // delete operation
        if (this?.contains(it.key) != true) {
            results[it.key] = null
        }
    }

    return results
}