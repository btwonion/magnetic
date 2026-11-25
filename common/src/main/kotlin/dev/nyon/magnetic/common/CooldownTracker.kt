package dev.nyon.magnetic.common

import java.util.concurrent.ConcurrentHashMap

class CooldownTracker<K : Any>(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val lastAcquiredAt = ConcurrentHashMap<K, Long>()

    fun tryAcquire(key: K, cooldownMillis: Long): Boolean {
        val now = currentTimeMillis()
        var acquired = false
        lastAcquiredAt.compute(key) { _, previous ->
            if (previous != null && now <= previous + cooldownMillis) {
                previous
            } else {
                acquired = true
                now
            }
        }
        return acquired
    }
}
