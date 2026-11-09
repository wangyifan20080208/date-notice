package com.smartagenda.ingest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class ImportJobStore(
    scope: CoroutineScope,
    private val useCase: ManualImportUseCase,
    private val notifier: ImportStatusNotifier,
) {

    private val _jobs = MutableStateFlow<List<ImportJob>>(emptyList())
    val jobs: StateFlow<List<ImportJob>> = _jobs.asStateFlow()

    private val queue = Channel<Long>(capacity = 64)
    private val nextId = AtomicLong(1L)

    init {
        scope.launch {
            for (jobId in queue) {
                val job = _jobs.value.firstOrNull { it.id == jobId } ?: continue
                update(jobId) { it.copy(status = ImportJobStatus.PARSING) }

                val result = runCatching {
                    useCase.importText(job.source, job.content)
                }.getOrElse {
                    ManualImportResult.Failed(it.message ?: "解析失败")
                }

                when (result) {
                    is ManualImportResult.Created -> {
                        update(jobId) {
                            it.copy(
                                status = ImportJobStatus.SUCCESS,
                                finishedAt = System.currentTimeMillis(),
                                createdTitles = result.titles,
                            )
                        }
                        notifier.notifyCompleted(
                            title = "解析完成",
                            detail = "已添加 ${result.titles.size} 项：" +
                                result.titles.joinToString("、").take(80),
                        )
                    }

                    is ManualImportResult.Updated -> {
                        update(jobId) {
                            it.copy(
                                status = ImportJobStatus.SUCCESS,
                                finishedAt = System.currentTimeMillis(),
                                updatedTaskId = result.id,
                            )
                        }
                        notifier.notifyCompleted("解析完成", "已更新 1 项日程")
                    }

                    ManualImportResult.Ignored -> {
                        update(jobId) {
                            it.copy(
                                status = ImportJobStatus.IGNORED,
                                finishedAt = System.currentTimeMillis(),
                            )
                        }
                    }

                    is ManualImportResult.Failed -> {
                        update(jobId) {
                            it.copy(
                                status = ImportJobStatus.FAILED,
                                finishedAt = System.currentTimeMillis(),
                                error = result.reason,
                            )
                        }
                        notifier.notifyCompleted("解析失败", result.reason)
                    }

                    ManualImportResult.NotConfigured -> {
                        update(jobId) {
                            it.copy(
                                status = ImportJobStatus.FAILED,
                                finishedAt = System.currentTimeMillis(),
                                error = "未配置大模型",
                            )
                        }
                        notifier.notifyCompleted("解析失败", "请先在设置里配置大模型")
                    }
                }
            }
        }
    }

    fun enqueue(source: String, content: String): Long {
        val id = nextId.getAndIncrement()
        val job = ImportJob(
            id = id,
            source = source.trim().ifBlank { "手动导入" },
            content = content.trim(),
            status = ImportJobStatus.QUEUED,
            createdAt = System.currentTimeMillis(),
        )
        _jobs.update { (listOf(job) + it).take(MAX_JOBS) }
        queue.trySend(id)
        return id
    }

    fun clearFinished() {
        _jobs.update { list ->
            list.filter {
                it.status == ImportJobStatus.QUEUED || it.status == ImportJobStatus.PARSING
            }
        }
    }

    private fun update(id: Long, transform: (ImportJob) -> ImportJob) {
        _jobs.update { list ->
            list.map { job -> if (job.id == id) transform(job) else job }
        }
    }

    companion object {
        private const val MAX_JOBS = 30
    }
}