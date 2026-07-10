package com.formuloo.os

import com.formuloo.core.database.DatabaseDriverFactory
import com.formuloo.core.database.JvmDatabaseDriverFactory
import com.formuloo.feature.gesdoc.util.CsvOpener
import com.formuloo.feature.hr.util.PdfOpener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.dsl.module
import java.awt.Desktop
import java.io.File

private class DesktopPdfOpener : PdfOpener {
    override suspend fun openPdf(bytes: ByteArray, filename: String) {
        withContext(Dispatchers.IO) {
            val tempFile = File(System.getProperty("java.io.tmpdir"), filename)
            tempFile.writeBytes(bytes)
            Desktop.getDesktop().open(tempFile)
        }
    }
}

private class DesktopCsvOpener : CsvOpener {
    override suspend fun openCsv(bytes: ByteArray, filename: String) {
        withContext(Dispatchers.IO) {
            val tempFile = File(System.getProperty("java.io.tmpdir"), filename)
            tempFile.writeBytes(bytes)
            Desktop.getDesktop().open(tempFile)
        }
    }
}

actual val platformModule = module {
    single<DatabaseDriverFactory> { JvmDatabaseDriverFactory() }
    single<PdfOpener> { DesktopPdfOpener() }
    single<CsvOpener> { DesktopCsvOpener() }
}
