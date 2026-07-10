package com.formuloo.core.common.notification

actual fun createNotificationService(): NotificationService = NoOpNotificationService()

private class NoOpNotificationService : NotificationService {
    override suspend fun notifyStatusChange(title: String, message: String) {
        println("[NotificationService no-op] $title — $message")
    }
}
