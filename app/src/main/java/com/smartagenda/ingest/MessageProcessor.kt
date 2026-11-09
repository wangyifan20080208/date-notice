package com.smartagenda.ingest

interface MessageProcessor {
    fun enqueue(message: IncomingMessage)
}