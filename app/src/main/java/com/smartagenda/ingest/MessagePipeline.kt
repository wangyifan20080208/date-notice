package com.smartagenda.ingest

import android.util.Log
import com.smartagenda.ai.AiDecision
import com.smartagenda.ai.AiParseEngine
import com.smartagenda.data.prefs.SettingsDataStore
import com.smartagenda.data.repository.TaskRepository
import kotlinx.coroutines.flow.first

class MessagePipeline(
    private val settingsStore: SettingsDataStore,
    private val aiEngine: AiParseEngine,
    private val repository: TaskRepository,
) {

    suspend fun handle(message: IncomingMessage) {
        val settings = settingsStore.settings.first()
        if (!settings.enabled || !settings.isConfigured) return
        if (message.source in settings.ignoredSources) return

        val now = System.currentTimeMillis()
        val existing = repository.pendingWithin(days = 7)

        when (val decision = aiEngine.parse(message, existing, now)) {
            is AiDecision.Create -> {
                decision.items.forEach { item ->
                    repository.create(
                        title = item.title,
                        datetime = item.datetime,
                        source = message.source,
                        sourceApp = message.app.name,
                        location = item.location,
                        note = message.content.take(200),
                        isTimeMissing = item.isTimeMissing,
                    )
                }
                Log.i(
                    TAG,
                    "已创建 ${decision.items.size} 条日程：" +
                        decision.items.joinToString("、") { it.title },
                )
            }

            is AiDecision.Update -> {
                if (existing.none { it.id == decision.id }) {
                    Log.w(TAG, "AI 返回的 UPDATE id=${decision.id} 不在上下文中，已忽略")
                    return
                }
                repository.updateFromAi(
                    id = decision.id,
                    title = decision.title,
                    datetime = decision.datetime,
                    location = decision.location,
                )
                Log.i(TAG, "已更新日程 id=${decision.id}")
            }

            AiDecision.Ignore -> Log.d(TAG, "AI 判定为无关消息，忽略")

            is AiDecision.Failure -> Log.w(TAG, "AI 解析失败：${decision.reason}")
        }
    }

    companion object {
        private const val TAG = "MessagePipeline"
    }
}