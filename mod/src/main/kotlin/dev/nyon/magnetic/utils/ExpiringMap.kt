package dev.nyon.magnetic.utils

/** A small expiring map with an injectable clock for deterministic tests. */
internal class ExpiringMap<K, V>(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private data class Entry<V>(val value: V, val expiresAt: Long)

    private val entries = mutableMapOf<K, Entry<V>>()

    fun put(key: K, value: V, timeoutMillis: Long) {
        entries[key] = Entry(value, currentTimeMillis() + timeoutMillis)
    }

    operator fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (currentTimeMillis() > entry.expiresAt) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    fun cleanup() {
        val now = currentTimeMillis()
        entries.values.removeIf { now > it.expiresAt }
    }

    internal fun size(): Int = entries.size
}
