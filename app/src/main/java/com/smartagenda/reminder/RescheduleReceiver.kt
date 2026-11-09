package com.smartagenda.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartagenda.data.local.AgendaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val tasks = AgendaDatabase.get(context).taskDao().allPending()
                ReminderScheduler(context).rescheduleAll(tasks)
                Log.i(TAG, "重注册闹钟完成 action=$action tasks=${tasks.size}")
            } catch (error: Exception) {
                Log.w(TAG, "重注册闹钟失败 action=$action", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "RescheduleReceiver"
        private const val EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            EXACT_ALARM_PERMISSION_CHANGED,
        )
    }
}