package com.formuloo.os

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.nio.file.Files
import javax.swing.JFileChooser

@Composable
actual fun rememberFilePickerLauncher(onFilePicked: (PickedFile) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch(Dispatchers.Swing) {
            val chooser = JFileChooser()
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                val bytes = file.readBytes()
                val mime = runCatching {
                    Files.probeContentType(file.toPath())
                }.getOrNull() ?: "application/octet-stream"
                onFilePicked(PickedFile(bytes, file.name, mime))
            }
        }
    }
}
