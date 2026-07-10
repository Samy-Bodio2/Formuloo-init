package com.formuloo.core.common.notification

import android.util.Log

actual fun createNotificationService(): NotificationService = NoOpNotificationService()

private class NoOpNotificationService : NotificationService {
    override suspend fun notifyStatusChange(title: String, message: String) {
        Log.d("NotificationService", "[no-op] $title — $message")
    }
}
