package dev.nyon.magnetic.compat

import java.util.UUID

internal data class TreeCapitatorBlockKey(
    val worldId: UUID,
    val x: Int,
    val y: Int,
    val z: Int
) {
    fun distanceSquared(other: TreeCapitatorBlockKey): Long {
        if (worldId != other.worldId) return Long.MAX_VALUE
        val dx = x.toLong() - other.x
        val dy = y.toLong() - other.y
        val dz = z.toLong() - other.z
        return dx * dx + dy * dy + dz * dz
    }
}

private const val MAX_ROOT_TO_MARKER_DISTANCE_SQUARED = 3L

/**
 * Selects the newest pending root adjacent to any marker in the current
 * TreeCapitator batch. TreeCapitator starts detection at the broken root and
 * creates its first log marker in one of the neighboring blocks.
 */
internal fun selectTreeCapitatorSessionIndex(
    sessionRoots: List<TreeCapitatorBlockKey>,
    markerPositions: Set<TreeCapitatorBlockKey>
): Int? {
    return sessionRoots.indices
        .asSequence()
        .mapNotNull { index ->
            val distance = markerPositions.minOfOrNull(sessionRoots[index]::distanceSquared)
                ?: return@mapNotNull null
            if (distance > MAX_ROOT_TO_MARKER_DISTANCE_SQUARED) return@mapNotNull null
            index to distance
        }
        .minWithOrNull(
            compareBy<Pair<Int, Long>> { (_, distance) -> distance }
                .thenByDescending { (index) -> index }
        )
        ?.first
}
