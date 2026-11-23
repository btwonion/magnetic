package dev.nyon.magnetic.holders

import java.util.UUID

/**
 * Attached to Server Tree Feller's queued log-break jobs so deferred breaks
 * can be attributed to the player who felled the tree.
 */
interface TreeFellerJobHolder {
    var magneticPlayer: UUID?
}
