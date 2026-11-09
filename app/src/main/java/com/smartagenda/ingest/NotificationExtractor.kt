package com.smartagenda.ingest

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.smartagenda.BuildConfig

internal object NotificationExtractor {

    private const val MAX_CONTENT_LENGTH = 1_200

    private val IGNORED_SOURCES = setOf(
        "微信团队", "QQ团队", "服务通知", "订阅号消息", "折叠的消息", "微信运动",
    )
    private val RESERVED_LABELS = setOf("时间", "地点", "主题", "备注", "会议", "内容")

    private val COUNT_PREFIX = Regex("""^\[\d+条]\s*""")
    private val SENDER_PREFIX = Regex("""^([^:：\n]{1,24})[:：]\s*""")
    private val MEDIA_ONLY = Regex("""^(\[[^]]+]\s*)+$""")

    fun extract(sbn: StatusBarNotification?): IncomingMessage? {
        val notification = sbn?.notification ?: return null
        val app = resolveApp(sbn.packageName) ?: return null
        if (sbn.isOngoing) return null
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

        val extras = notification.extras ?: return null

        val source = firstNonBlank(
            extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TITLE_BIG),
        ) ?: return null
        if (source in IGNORED_SOURCES) return null

        val raw = firstNonBlank(
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            latestMessageText(extras),
        ) ?: return null

        val body = raw.replace(COUNT_PREFIX, "").trim()
        if (body.isEmpty() || MEDIA_ONLY.matches(body)) return null

        val senderMatch = SENDER_PREFIX.find(body)
        val candidate = senderMatch?.groupValues?.getOrNull(1)?.trim()
        val sender = candidate?.takeIf { it.isNotBlank() && it !in RESERVED_LABELS }
        val content = if (sender != null) {
            senderMatch?.let { body.removeRange(it.range).trim() } ?: body
        } else {
            body
        }
        if (content.length < 2) return null

        return IncomingMessage(
            app = app,
            source = source,
            content = content.take(MAX_CONTENT_LENGTH),
            sender = sender,
            receivedAt = sbn.postTime,
            notificationKey = sbn.key,
        )
    }

    private fun latestMessageText(extras: Bundle): CharSequence? {
        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
        val last = messages.lastOrNull() as? Bundle ?: return null
        return last.getCharSequence("text")
    }

    private fun firstNonBlank(vararg values: CharSequence?): String? =
        values.asSequence()
            .mapNotNull { it?.toString()?.trim() }
            .firstOrNull { it.isNotEmpty() }

    private fun resolveApp(packageName: String): SourceApp? =
        SourceApp.fromPackage(packageName)
            ?: if (BuildConfig.DEBUG && packageName == "com.android.shell") {
                SourceApp.WECHAT
            } else {
                null
            }
}