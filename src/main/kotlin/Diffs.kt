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
        if (old == null || prop.get(old) != newValue) {
            results[prop.name] = newValue
        }
    }

    return results
}


/**
 * As simpleDiff, but recursive. At each level, the next call can be either simpleDiff (if all members are primitives),
 * or a mix of calls to simpleDiff and nestedDiff
 */
interface Diffable<T> {
    fun diff(existing: T?) : Map<String, Any?> = simpleDiff(old=existing, new=this)
}

// KProperty1<S, T?> means - a property on an object of type S, which is of the type T?
inline fun <reified S, reified T : Diffable<T>> nestedDiff(old: S?, new: S, prop: KProperty1<S, T?>) : Map<String, Any?> {
    val oldValue = old?.let { prop.get(it) }

    // if both new and old are null, return nothing. If new is null but old isnt, return propName -> null
    val newValue = prop.get(new) ?: return if (oldValue == null) emptyMap() else mapOf(prop.name to null)

    // else recurse. Add the property name to the front of each returned result (so we get a label of A.B.C)
    return newValue.diff(oldValue).mapKeys { "${prop.name}.${it.key}" }
}

/*inline fun <reified S, reified T : Diffable<T>> arrayDiff(old: S?, new: S, prop: KProperty1<S, List<T?>>) : Map<String, Any?> {
    val oldValue = old?.let { prop.get(it) }

    // if both new and old are empty or null, return nothing. If new is null but old isnt, return propName -> null
    val newValue = prop.get(new)

    if (newValue.isEmpty()) return if (oldValue.isNullOrEmpty()) emptyMap() else mapOf(prop.name to null)

    // else recurse. Add the property name to the front of each returned result (so we get a label of A.B.C)
    return newValue.diff(oldValue).mapKeys { "${prop.name}.${it.key}" }
}*/