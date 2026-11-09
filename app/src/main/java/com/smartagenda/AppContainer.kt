package com.smartagenda

import android.content.Context
import com.smartagenda.ai.AiParseEngine
import com.smartagenda.data.local.AgendaDatabase
import com.smartagenda.data.prefs.SettingsDataStore
import com.smartagenda.data.repository.TaskRepository
import com.smartagenda.ingest.ImportJobStore
import com.smartagenda.ingest.ImportStatusNotifier
import com.smartagenda.ingest.MessagePipeline
import com.smartagenda.ingest.MessageProcessingQueue
import com.smartagenda.ingest.MessageProcessor
import com.smartagenda.reminder.ReminderNotifier
import com.smartagenda.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsStore = SettingsDataStore(appContext)

    private val database = AgendaDatabase.get(appContext)

    val reminderScheduler = ReminderScheduler(appContext)

    val reminderNotifier = ReminderNotifier(appContext)

    val taskRepository = TaskRepository(
        dao = database.taskDao(),
        scheduler = reminderScheduler,
        notifier = reminderNotifier,
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val aiParseEngine = AiParseEngine(settingsStore, okHttpClient)

    val manualImport = com.smartagenda.ingest.ManualImportUseCase(
        settingsStore = settingsStore,
        aiEngine = aiParseEngine,
        repository = taskRepository,
    )

    val importStatusNotifier = ImportStatusNotifier(appContext)

    val importJobStore = ImportJobStore(
        scope = appScope,
        useCase = manualImport,
        notifier = importStatusNotifier,
    )

    private val pipeline = MessagePipeline(
        settingsStore = settingsStore,
        aiEngine = aiParseEngine,
        repository = taskRepository,
    )

    val messageProcessor: MessageProcessor = MessageProcessingQueue(appScope) { message ->
        pipeline.handle(message)
    }

    fun initialize() {
        reminderNotifier.ensureChannel()
        importStatusNotifier.ensureChannel()
        appScope.launch {
            taskRepository.allPending().forEach(reminderScheduler::schedule)
        }
    }
}