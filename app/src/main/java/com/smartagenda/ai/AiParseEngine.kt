package com.smartagenda.ai

import com.smartagenda.core.time.TaskTimeParser
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.data.prefs.AppSettings
import com.smartagenda.data.prefs.SettingsDataStore
import com.smartagenda.ingest.IncomingMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

interface ChatApi {
    @POST
    suspend fun chat(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest,
    ): ChatResponse
}

class AiParseEngine(
    private val settingsStore: SettingsDataStore,
    okHttpClient: OkHttpClient,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val api: ChatApi = Retrofit.Builder()
        .baseUrl("https://api.deepseek.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ChatApi::class.java)

    suspend fun parse(
        message: IncomingMessage,
        existingTasks: List<TaskEntity>,
        now: Long,
    ): AiDecision = withContext(Dispatchers.IO) {
        val settings = settingsStore.settings.first()
        parseInternal(message, existingTasks, now, settings)
    }

    suspend fun testConnection(settings: AppSettings): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.chat(
                    url = endpointOf(settings.baseUrl),
                    authorization = "Bearer ${settings.apiKey}",
                    request = ChatRequest(
                        model = settings.model,
                        messages = listOf(
                            ChatMessage(role = "system", content = "只回复两个字：可用"),
                            ChatMessage(role = "user", content = "连通性测试"),
                        ),
                        maxTokens = 200,
                    ),
                )
                val message = response.choices.firstOrNull()?.message
                message?.content?.takeIf { it.isNotBlank() }
                    ?: message?.reasoningContent?.takeIf { it.isNotBlank() }
                    ?: message?.reasoning?.takeIf { it.isNotBlank() }
                    ?: response.error?.message
                    ?: "服务返回为空"
            }
        }

    suspend fun parseManual(
        sourceLabel: String,
        content: String,
        existingTasks: List<TaskEntity>,
        now: Long,
    ): AiDecision = withContext(Dispatchers.IO) {
        val settings = settingsStore.settings.first()
        if (!settings.enabled) return@withContext AiDecision.Failure("AI 解析已关闭")
        if (!settings.isConfigured) return@withContext AiDecision.Failure("尚未配置 API Key")

        val system = PromptBuilder.systemPrompt(now, TaskRepositoryJson.of(existingTasks))
        val user = PromptBuilder.userPrompt(
            source = sourceLabel,
            sourceAppLabel = "手动导入",
            sender = null,
            content = content,
        )
        normalize(executeChat(system, user, settings), allowPastAsMissing = true)
    }

    private suspend fun parseInternal(
        message: IncomingMessage,
        existingTasks: List<TaskEntity>,
        now: Long,
        settings: AppSettings,
    ): AiDecision {
        if (!settings.enabled) return AiDecision.Failure("AI 解析已关闭")
        if (!settings.isConfigured) return AiDecision.Failure("尚未配置 API Key")

        val system = PromptBuilder.systemPrompt(now, TaskRepositoryJson.of(existingTasks))
        val user = PromptBuilder.userPrompt(
            source = message.source,
            sourceAppLabel = message.app.displayName,
            sender = message.sender,
            content = message.content,
        )
        return executeChat(system, user, settings).let {
            normalize(it, allowPastAsMissing = false)
        }
    }

    private fun normalize(decision: AiDecision, allowPastAsMissing: Boolean): AiDecision {
        val now = System.currentTimeMillis()
        if (decision is AiDecision.Update && decision.datetime != null && decision.datetime <= now) {
            return AiDecision.Ignore
        }
        if (decision !is AiDecision.Create) return decision

        val items = decision.items.mapNotNull { item ->
            val datetime = item.datetime
            if (datetime != null && datetime <= now) {
                if (allowPastAsMissing) {
                    item.copy(datetime = null, isTimeMissing = true)
                } else {
                    null
                }
            } else {
                item
            }
        }
        return if (items.isEmpty()) AiDecision.Ignore else AiDecision.Create(items)
    }

    private suspend fun executeChat(
        system: String,
        user: String,
        settings: AppSettings,
    ): AiDecision {
        return try {
            val response = requestChat(settings, system, user)
            val assistant = response.choices.firstOrNull()?.message
            val text = assistant?.content?.takeIf { it.isNotBlank() }
                ?: assistant?.reasoningContent?.takeIf { it.isNotBlank() }
                ?: assistant?.reasoning?.takeIf { it.isNotBlank() }
                ?: return AiDecision.Failure(response.error?.message ?: "模型返回为空")
            decodeDecision(text)
        } catch (http: HttpException) {
            AiDecision.Failure("HTTP ${http.code()}: ${http.message()}")
        } catch (error: Exception) {
            AiDecision.Failure(error.message ?: error.javaClass.simpleName)
        }
    }

    private suspend fun requestChat(
        settings: AppSettings,
        system: String,
        user: String,
    ): ChatResponse {
        val request = ChatRequest(
            model = settings.model,
            messages = listOf(
                ChatMessage(role = "system", content = system),
                ChatMessage(role = "user", content = user),
            ),
            maxTokens = 1500,
        )

        var attempt = 0
        while (true) {
            try {
                return api.chat(
                    url = endpointOf(settings.baseUrl),
                    authorization = "Bearer ${settings.apiKey}",
                    request = request,
                )
            } catch (http: HttpException) {
                if (attempt < MAX_RETRIES && http.code() in RETRYABLE_CODES) {
                    attempt++
                    delay(RETRY_DELAY_MS * attempt)
                } else {
                    throw http
                }
            }
        }
    }

    private fun decodeDecision(raw: String): AiDecision {
        var result: AiDecision? = null
        extractJsonCandidates(raw).forEach { candidate ->
            val dto = runCatching {
                json.decodeFromString<AiCommandDto>(candidate)
            }.getOrNull() ?: return@forEach
            result = toDecision(dto) ?: result
        }
        return result ?: AiDecision.Ignore
    }

    private fun toDecision(dto: AiCommandDto): AiDecision? {
        return when (dto.action.uppercase()) {
"CREATE", "CREATE_MANY" -> {
            val payloads = dto.tasks?.takeIf { it.isNotEmpty() }
                ?: listOfNotNull(dto.effectiveTask())
            val items = payloads.mapNotNull { payload ->
                val title = payload.title?.trim().orEmpty()
                if (title.isEmpty()) return@mapNotNull null
                val parsedTime = TaskTimeParser.parse(payload.datetime)
                CreatedTask(
                    title = title.take(60),
                    datetime = parsedTime,
                    location = payload.location?.trim()?.takeIf { it.isNotEmpty() },
                    isTimeMissing = payload.isTimeMissing || parsedTime == null,
                )
            }.take(MAX_TASKS_PER_MESSAGE)
            if (items.isEmpty()) null else AiDecision.Create(items)
        }

            "UPDATE" -> {
                val payload = dto.effectiveTask() ?: return null
                val id = payload.id ?: return null
                AiDecision.Update(
                    id = id,
                    title = payload.title?.trim()?.takeIf { it.isNotEmpty() },
                    datetime = TaskTimeParser.parse(payload.datetime),
                    location = payload.location?.trim()?.takeIf { it.isNotEmpty() },
                )
            }

            "IGNORE" -> AiDecision.Ignore
            else -> null
        }
    }

    private fun extractJsonCandidates(raw: String): List<String> {
        val candidates = mutableListOf<String>()

        Regex("```(?:json)?\\s*([\\s\\S]*?)```").findAll(raw).forEach { match ->
            match.groupValues[1].trim()
                .takeIf { it.startsWith("{") }
                ?.let(candidates::add)
        }

        var depth = 0
        var start = -1
        var inString = false
        var escaped = false

        raw.forEachIndexed { index, char ->
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    '{' -> {
                        if (depth == 0) start = index
                        depth++
                    }

                    '}' -> {
                        if (depth > 0) {
                            depth--
                            if (depth == 0 && start >= 0) {
                                candidates.add(raw.substring(start, index + 1))
                            }
                        }
                    }
                }
            }
        }

        return candidates.distinct()
    }

    private fun endpointOf(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return "$trimmed/chat/completions"
    }

    companion object {
        private const val MAX_RETRIES = 1
        private const val RETRY_DELAY_MS = 700L
        private const val MAX_TASKS_PER_MESSAGE = 5
        private val RETRYABLE_CODES = setOf(408, 429, 500, 502, 503, 504)
    }
}

private object TaskRepositoryJson {
    fun of(tasks: List<TaskEntity>): String = buildString {
        append('[')
        tasks.forEachIndexed { index, task ->
            if (index > 0) append(',')
            append("{\"id\":").append(task.id)
            append(",\"title\":\"").append(task.title.escape()).append('"')
            append(",\"datetime\":")
            if (task.datetime == null) {
                append("null")
            } else {
                append('"').append(TimeFormatter.isoLocal(task.datetime)).append('"')
            }
            append(",\"source\":\"").append(task.source.escape()).append('"')
            append(",\"location\":")
            if (task.location == null) {
                append("null")
            } else {
                append('"').append(task.location.escape()).append('"')
            }
            append(",\"isTimeMissing\":").append(task.isTimeMissing)
            append('}')
        }
        append(']')
    }

    private fun String.escape(): String = buildString {
        this@escape.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}