package com.smartagenda.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartagenda.data.local.AgendaDatabase
import com.smartagenda.data.local.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                AgendaDatabase.get(context).taskDao().updateStatus(
                    taskId,
                    TaskStatus.DONE,
                    System.currentTimeMillis(),
                )
                ReminderScheduler(context).cancel(taskId)
                ReminderNotifier(context).cancel(taskId)
                Log.i(TAG, "通知内完成日程 task=$taskId")
            } catch (error: Exception) {
                Log.w(TAG, "通知内完成失败 task=$taskId", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderActionReceiver"
        const val ACTION_COMPLETE = "com.smartagenda.action.COMPLETE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}