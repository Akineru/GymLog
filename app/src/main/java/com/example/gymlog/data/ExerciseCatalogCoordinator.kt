package com.example.gymlog.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared single-flight lock for the whole local exercise catalog lifecycle:
 * startup import, backup restore, clear-and-reseed and catalog maintenance.
 *
 * The lock is deliberately coarse-grained. It covers the snapshot/capture,
 * database transaction and rollback of a restore, not only one SQL statement.
 */
class ExerciseCatalogCoordinator {
    private val mutex = Mutex()

    internal suspend fun <T> withLock(block: suspend () -> T): T =
        mutex.withLock { block() }
}
