package com.smartagenda.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smartagenda.MainActivity
import com.smartagenda.R
import com.smartagenda.core.time.TimeFormatter
import com.smartagenda.data.local.TaskEntity

class ReminderNotifier(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_reminder_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_reminder_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun show(task: TaskEntity) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_TASK_ID, task.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val completeIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt() + COMPLETE_REQUEST_OFFSET,
            Intent(context, ReminderActionReceiver::class.java)
                .setAction(ReminderActionReceiver.ACTION_COMPLETE)
                .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val timeText = task.datetime?.let { TimeFormatter.formatFull(it) }.orEmpty()
        val body = buildString {
            if (timeText.isNotEmpty()) append(timeText)
            if (task.source.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(task.source)
            }
            if (task.location?.isNotBlank() == true) {
                if (isNotEmpty()) append(" · ")
                append(task.location)
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(task.title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setWhen(task.datetime ?: System.currentTimeMillis())
            .setShowWhen(task.datetime != null)

        builder.addAction(
            0,
            context.getString(
                if (task.isTimeMissing) R.string.notif_action_missing_time else R.string.notif_action_open,
            ),
            openIntent,
        )

        builder.addAction(
            0,
            context.getString(R.string.notif_action_complete),
            completeIntent,
        )

        notificationManager.notify(task.id.toInt(), builder.build())
    }

    fun cancel(taskId: Long) {
        notificationManager.cancel(taskId.toInt())
    }

    companion object {
        const val CHANNEL_ID = "agenda_reminder"
        private const val COMPLETE_REQUEST_OFFSET = 100_000
    }
}