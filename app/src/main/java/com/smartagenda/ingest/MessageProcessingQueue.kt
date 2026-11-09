package com.smartagenda.ingest

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MessageProcessingQueue(
    scope: CoroutineScope,
    private val handler: suspend (IncomingMessage) -> Unit,
) : MessageProcessor {

    private val queue = Channel<IncomingMessage>(
        capacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        scope.launch {
            for (message in queue) {
                withTimeoutOrNull(PROCESS_TIMEOUT_MS) {
                    runCatching { handler(message) }
                        .onFailure { Log.w(TAG, "消息处理失败：${message.source}", it) }
                }
            }
        }
    }

    override fun enqueue(message: IncomingMessage) {
        queue.trySend(message)
    }

    companion object {
        private const val TAG = "MessageQueue"
        private const val PROCESS_TIMEOUT_MS = 60_000L
    }
}