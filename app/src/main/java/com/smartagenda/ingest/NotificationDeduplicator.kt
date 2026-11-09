package com.smartagenda.ingest

import android.os.SystemClock

internal object NotificationDeduplicator {

    private const val WINDOW_MS = 90_000L
    private const val MAX_ENTRIES = 200

    private val seen = LinkedHashMap<Int, Long>(32, 0.75f)

    @Synchronized
    fun shouldProcess(message: IncomingMessage): Boolean {
        val now = SystemClock.elapsedRealtime()

        val iterator = seen.entries.iterator()
        while (iterator.hasNext() && now - iterator.next().value > WINDOW_MS) {
            iterator.remove()
        }
        while (seen.size > MAX_ENTRIES) {
            val eldest = seen.keys.firstOrNull() ?: break
            seen.remove(eldest)
        }

        val key = 31 * message.app.hashCode() +
            31 * message.source.hashCode() +
            message.content.hashCode()

        if (seen.containsKey(key)) return false
        seen[key] = now
        return true
    }
}