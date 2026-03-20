package dev.nyon.magnetic.holders

import dev.nyon.magnetic.utils.PositionTracker

interface ServerLevelHolder {
    val positionTracker: PositionTracker
}
