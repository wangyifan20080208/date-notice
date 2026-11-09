package com.smartagenda.ingest

enum class ImportJobStatus { QUEUED, PARSING, SUCCESS, IGNORED, FAILED }

data class ImportJob(
    val id: Long,
    val source: String,
    val content: String,
    val status: ImportJobStatus,
    val createdAt: Long,
    val finishedAt: Long? = null,
    val createdTitles: List<String> = emptyList(),
    val updatedTaskId: Long? = null,
    val error: String? = null,
) {
    val summary: String
        get() = content.replace('\n', ' ').trim().take(80)
}