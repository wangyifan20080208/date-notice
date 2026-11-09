package com.smartagenda.ingest

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smartagenda.AgendaApp

class GroupNotificationListener : NotificationListenerService() {

    private val processor: MessageProcessor
        get() = (application as AgendaApp).container.messageProcessor

    override fun onListenerConnected() {
        Log.i(TAG, "通知监听已连接")
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "通知监听被系统解绑，尝试重新绑定")
        NotificationListenerService.requestRebind(
            ComponentName(this, GroupNotificationListener::class.java),
        )
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
    ) {
        val message = NotificationExtractor.extract(sbn) ?: return
        if (!NotificationDeduplicator.shouldProcess(message)) return

        Log.d(TAG, "拦截 ${message.app.displayName} / ${message.source}")
        processor.enqueue(message)
    }

    companion object {
        private const val TAG = "GroupNotifListener"
    }
}