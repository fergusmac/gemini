
infix fun String.dot(other: String) : String {
    return "$this.$other"
}

fun String.firstDot() : String {
    return this.split('.')[0]
}

/**
 * Add all the entries to the map, but add a prefix (ending in a .) to each key
 */
fun <V> MutableMap<String, V?>.putAllPrefixed(prefix: String, items: Map<String, V>?) {
    if (items == null)
    {
        this[prefix] = null
        return
    }

    this.putAll(items.mapKeys { prefix dot it.key })
}

fun String?.nullIfBlank() : String? {
    return this?.let { it.ifBlank { null } }
}

/** Copies the list, inserting a new element, or updating it if a matching element already exists
 * If the list is null, create a new list.
 * upsertFunc takes in the existing element, if any
 */
fun <T: Any> List<T>?.upsertElement(filtr : (T) -> Boolean, upsertFunc : (T?) -> T, requireExisting : Boolean = false) : List<T> {

    val result = this?.toMutableList() ?: mutableListOf()

    val existing = result.find(filtr)?.also { result.remove(it) }

    if (existing == null && requireExisting) return result

    result.add(upsertFunc(existing))

    return result
}

fun <T> List<T>.isSortedWith(comparator: Comparator<T>): Boolean {
    if (size < 2) return true
    for (i in 1..<size) {
        if (comparator.compare(this[i - 1], this[i]) > 0) {
            return false
        }
    }
    return true
}