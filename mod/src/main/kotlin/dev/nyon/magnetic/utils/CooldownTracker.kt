package dev.nyon.magnetic.utils

internal class CooldownTracker<K>(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val lastAcquiredAt = mutableMapOf<K, Long>()

    fun tryAcquire(key: K, cooldownMillis: Long): Boolean {
        val now = currentTimeMillis()
        val previous = lastAcquiredAt[key]
        if (previous != null && now <= previous + cooldownMillis) return false

        lastAcquiredAt[key] = now
        return true
    }

    fun clear() = lastAcquiredAt.clear()
}
