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

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val task = AgendaDatabase.get(context).taskDao().findById(taskId)
                if (task != null && task.status == TaskStatus.PENDING) {
                    ReminderNotifier(context).show(task)
                }
            } catch (error: Exception) {
                Log.w(TAG, "处理提醒失败 task=$taskId", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderReceiver"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}