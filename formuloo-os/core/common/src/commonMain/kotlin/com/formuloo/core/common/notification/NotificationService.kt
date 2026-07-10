package com.formuloo.core.common.notification

/**
 * Service de notification — interface stable, indépendante du fournisseur push.
 *
 * Implémentation actuelle (toutes plateformes) : no-op, se contente de logger le message.
 * L'intégration réelle FCM (Android) / APNs (iOS) fera l'objet d'un ticket séparé ;
 * cette interface ne devra pas changer, seules les actuals seront remplacées.
 */
interface NotificationService {
    suspend fun notifyStatusChange(title: String, message: String)
}

expect fun createNotificationService(): NotificationService
