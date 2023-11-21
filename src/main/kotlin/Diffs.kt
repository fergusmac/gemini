import kotlin.reflect.full.declaredMemberProperties


inline fun <reified T: Any> memberDiff(old : T?, new : T?, skip : Set<String> = emptySet()) : Map<String, Any?>? {
    val results = mutableMapOf<String, Any?>()

    if (old == new) return emptyMap()

    if (new == null) return null

    for (prop in T::class.declaredMemberProperties) {

        if (prop.name in skip) continue

        val newValue = new.let(prop)
        val oldValue = old?.let(prop)

        if(oldValue == newValue) continue

        results[prop.name] = newValue
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