package com.formuloo.os

import androidx.compose.runtime.Composable

data class PickedFile(
    val bytes: ByteArray,
    val name: String,
    val mimeType: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedFile) return false
        return name == other.name && mimeType == other.mimeType
    }
    override fun hashCode(): Int = name.hashCode() * 31 + mimeType.hashCode()
}

/**
 * Retourne une lambda qui, lorsqu'elle est invoquée, ouvre le sélecteur de
 * fichier natif de la plateforme.  Le résultat est transmis via [onFilePicked].
 *
 * Doit être appelée depuis un contexte @Composable (NavHost, etc.).
 */
@Composable
expect fun rememberFilePickerLauncher(onFilePicked: (PickedFile) -> Unit): () -> Unit
