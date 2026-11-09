package com.smartagenda.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    @SerialName("reasoning") val reasoning: String? = null,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.1,
    @SerialName("max_tokens") val maxTokens: Int = 1500,
    val stream: Boolean = false,
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
    val error: ChatError? = null,
)

@Serializable
data class ChatChoice(
    val message: ChatMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class ChatError(
    val message: String? = null,
    val type: String? = null,
)

@Serializable
data class AiCommandDto(
    val action: String = "IGNORE",
    val task: AiTaskPayload? = null,
    val tasks: List<AiTaskPayload>? = null,
    val id: Long? = null,
    val title: String? = null,
    val datetime: String? = null,
    val location: String? = null,
    @SerialName("isTimeMissing") val isTimeMissing: Boolean = false,
) {
    fun effectiveTask(): AiTaskPayload? = task ?: run {
        if (title == null && datetime == null && id == null) {
            null
        } else {
            AiTaskPayload(
                id = id,
                title = title,
                datetime = datetime,
                location = location,
                isTimeMissing = isTimeMissing,
            )
        }
    }
}

@Serializable
data class AiTaskPayload(
    val id: Long? = null,
    val title: String? = null,
    val datetime: String? = null,
    val location: String? = null,
    @SerialName("isTimeMissing") val isTimeMissing: Boolean = false,
)