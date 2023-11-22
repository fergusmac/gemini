import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf

inline fun <reified T: Any> memberDiff(old : T?, new : T?, skip : Set<String> = emptySet()) : Map<String, Any?>? {
    val results = mutableMapOf<String, Any?>()

    if (old == new) return emptyMap()

    if (new == null) return null

    for (prop in T::class.declaredMemberProperties) {

        if (prop.name in skip) continue

        val newValue = new.let(prop)
        val oldValue = old?.let(prop)

        if(oldValue == newValue) continue // no change

        assert(oldValue == null || newValue == null || oldValue::class == newValue::class)

        if (newValue is Diffable) {
            val propResults = newValue.diff(oldValue)
            results.putAllPrefixed(prefix=prop.name, items=propResults)
        }
        else if (newValue is Map<*, *>) {
            // at runtime we cant infer the types, but *I* know that any Map in here is always
            // a String to an Any?, so the cast is 'safe'
            val mapResults = mapDiff(oldValue as Map<String, Any?>?, newValue as Map<String, Any?>, name = prop.name)
            results.putAll(mapResults)
        }
        else {
            results[prop.name] = newValue
        }
    }

    return results
}

fun mapDiff(old: Map<String, Any?>?, new : Map<String, Any?>?,  name: String) : Map<String, Any?> {

    val results = mutableMapOf<String, Any?>()

    if (old.isNullOrEmpty() && new.isNullOrEmpty()){
        return emptyMap()
    }
    else {

        new?.forEach {
            // create or modify operation (or nothing)
            val oldValue = old?.getOrDefault(it.key, null)
            if (oldValue != it.value) {
                results[it.key] = it.value
                if (it.value is Diffable) {
                    val propResults = (it.value as Diffable).diff(oldValue)
                    results.putAllPrefixed(prefix=it.key, items=propResults)
                }
                else {
                    results[it.key] = it.value
                }
            }
        }

        old?.forEach {
            // delete operation
            if (new?.contains(it.key) != true) {
                results[it.key] = null
            }
        }

    }

    return results.mapKeys { name dot it.key }
}

interface Diffable {

    //Use Any as the type, and do an unchecked cast in the implementing class
    //We can't specify the type here using a generic type, because casting to
    //Diffable<T> in memberDiff is not possible (the compiler can't infer what T is)
    fun diff(other: Any?) : Map<String, Any?>?
}