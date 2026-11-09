package com.smartagenda.ingest

data class IncomingMessage(
    val app: SourceApp,
    val source: String,
    val content: String,
    val sender: String?,
    val receivedAt: Long,
    val notificationKey: String,
)