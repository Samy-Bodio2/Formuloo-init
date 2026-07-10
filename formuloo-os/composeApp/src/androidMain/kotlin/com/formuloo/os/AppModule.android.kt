package com.formuloo.os

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.formuloo.core.database.AndroidDatabaseDriverFactory
import com.formuloo.core.database.DatabaseDriverFactory
import com.formuloo.feature.gesdoc.util.CsvOpener
import com.formuloo.feature.hr.util.PdfOpener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.dsl.module
import java.io.File

private class AndroidPdfOpener(private val context: Context) : PdfOpener {
    override suspend fun openPdf(bytes: ByteArray, filename: String) {
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, filename).also { it.writeBytes(bytes) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private class AndroidCsvOpener(private val context: Context) : CsvOpener {
    override suspend fun openCsv(bytes: ByteArray, filename: String) {
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, filename).also { it.writeBytes(bytes) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

actual val platformModule = module {
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(get<Context>()) }
    single<PdfOpener> { AndroidPdfOpener(get<Context>()) }
    single<CsvOpener> { AndroidCsvOpener(get<Context>()) }
}
