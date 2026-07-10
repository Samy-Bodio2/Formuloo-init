package com.formuloo.feature.hr.util

interface PdfOpener {
    suspend fun openPdf(bytes: ByteArray, filename: String)
}
