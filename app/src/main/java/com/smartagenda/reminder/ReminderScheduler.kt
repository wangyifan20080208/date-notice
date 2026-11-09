package com.smartagenda.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.smartagenda.MainActivity
import com.smartagenda.data.local.TaskEntity
import com.smartagenda.data.local.TaskStatus
import java.util.Calendar

class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    fun schedule(task: TaskEntity) {
        val triggerAt = task.datetime ?: return
        if (task.status != TaskStatus.PENDING) {
            cancel(task.id)
            return
        }
        if (triggerAt <= System.currentTimeMillis()) return

        val requestCode = task.id.toInt()
        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java)
                .putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TASK_ID, task.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, operation)
            Log.d(TAG, "已设定精确闹钟 task=${task.id} at=$triggerAt")
        } catch (security: SecurityException) {
            Log.w(TAG, "缺少精确闹钟权限，降级为非精确闹钟", security)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                operation,
            )
        }
    }

    fun cancel(taskId: Long) {
        val operation = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        operation?.let { alarmManager.cancel(it) }
    }

    fun rescheduleAll(tasks: List<TaskEntity>) {
        tasks.filter { it.status == TaskStatus.PENDING }.forEach(::schedule)
    }

    companion object {
        private const val TAG = "ReminderScheduler"

        fun canScheduleExact(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        }

        fun nextMinuteFromNow(): Long {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, 1)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    }
}