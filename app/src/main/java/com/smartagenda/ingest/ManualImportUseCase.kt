package com.smartagenda.ingest

import com.smartagenda.ai.AiDecision
import com.smartagenda.ai.AiParseEngine
import com.smartagenda.data.prefs.SettingsDataStore
import com.smartagenda.data.repository.TaskRepository
import kotlinx.coroutines.flow.first

sealed interface ManualImportResult {
    data class Created(val titles: List<String>) : ManualImportResult
    data class Updated(val id: Long) : ManualImportResult
    data object Ignored : ManualImportResult
    data class Failed(val reason: String) : ManualImportResult
    data object NotConfigured : ManualImportResult
}

class ManualImportUseCase(
    private val settingsStore: SettingsDataStore,
    private val aiEngine: AiParseEngine,
    private val repository: TaskRepository,
) {

    suspend fun importText(source: String, content: String): ManualImportResult {
        val text = content.trim()
        if (text.isEmpty()) return ManualImportResult.Ignored

        val settings = settingsStore.settings.first()
        if (!settings.enabled || !settings.isConfigured) {
            return ManualImportResult.NotConfigured
        }

        val now = System.currentTimeMillis()
        val existing = repository.pendingWithin(7)
        val sourceLabel = source.trim().ifBlank { "手动导入" }

        return when (val decision = aiEngine.parseManual(sourceLabel, text, existing, now)) {
            is AiDecision.Create -> {
                val titles = decision.items.map { item ->
                    repository.create(
                        title = item.title,
                        datetime = item.datetime,
                        source = sourceLabel,
                        sourceApp = "MANUAL",
                        location = item.location,
                        note = text.take(200),
                        isTimeMissing = item.isTimeMissing,
                    )
                    item.title
                }
                ManualImportResult.Created(titles)
            }

            is AiDecision.Update -> {
                if (existing.none { it.id == decision.id }) {
                    ManualImportResult.Failed("AI 返回了不存在的任务 id，已忽略")
                } else {
                    repository.updateFromAi(
                        id = decision.id,
                        title = decision.title,
                        datetime = decision.datetime,
                        location = decision.location,
                    )
                    ManualImportResult.Updated(decision.id)
                }
            }

            AiDecision.Ignore -> ManualImportResult.Ignored

            is AiDecision.Failure -> ManualImportResult.Failed(decision.reason)
        }
    }
}