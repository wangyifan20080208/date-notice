package com.smartagenda.ai

sealed interface AiDecision {

    data class Create(val items: List<CreatedTask>) : AiDecision

    data class Update(
        val id: Long,
        val title: String?,
        val datetime: Long?,
        val location: String?,
    ) : AiDecision

    data object Ignore : AiDecision

    data class Failure(val reason: String) : AiDecision
}

data class CreatedTask(
    val title: String,
    val datetime: Long?,
    val location: String?,
    val isTimeMissing: Boolean,
)