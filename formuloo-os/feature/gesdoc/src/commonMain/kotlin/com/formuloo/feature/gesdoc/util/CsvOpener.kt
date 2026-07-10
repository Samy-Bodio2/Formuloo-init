package com.formuloo.feature.gesdoc.util

interface CsvOpener {
    suspend fun openCsv(bytes: ByteArray, filename: String)
}
