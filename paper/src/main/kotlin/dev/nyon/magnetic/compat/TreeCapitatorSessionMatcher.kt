package dev.nyon.magnetic.compat

import dev.nyon.magnetic.extensions.BlockKey

private const val MAX_ROOT_TO_MARKER_DISTANCE_SQUARED = 3L

/**
 * Selects the newest pending root adjacent to any marker in the current
 * TreeCapitator batch. TreeCapitator starts detection at the broken root and
 * creates its first log marker in one of the neighboring blocks.
 */
internal fun selectTreeCapitatorSessionIndex(
    sessionRoots: List<BlockKey>,
    markerPositions: Set<BlockKey>
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
