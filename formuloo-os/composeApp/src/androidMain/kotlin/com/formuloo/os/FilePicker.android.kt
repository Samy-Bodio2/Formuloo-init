package com.formuloo.os

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFilePickerLauncher(onFilePicked: (PickedFile) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = context.contentResolver
            .openInputStream(uri)
            ?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        val name = context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val col = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && col >= 0) cursor.getString(col) else null
            }
            ?: uri.lastPathSegment
            ?: "document"
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        onFilePicked(PickedFile(bytes, name, mime))
    }
    return { launcher.launch("*/*") }
}
