
infix fun String.dot(other: String) : String {
    return "$this.$other"
}

fun String.firstDot() : String {
    return this.split('.')[0]
}

/**
 * Add all the entries to the map, but add a prefix (ending in a .) to each key
 */
fun <V> MutableMap<String, V?>.putAllPrefixed(prefix: String, putMe: Map<String, V>?) {
    if (putMe == null)
    {
        this[prefix] = null
        return
    }

    this.putAll(putMe.mapKeys { prefix dot it.key })
}

fun String?.nullIfBlank() : String? {
    return this?.let { it.ifBlank { null } }
}