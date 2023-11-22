
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