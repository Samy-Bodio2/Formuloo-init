package com.formuloo.core.common

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T, val code: Int = 200) : NetworkResult<T>
    data class Error(
        val message: String,
        val code: Int? = null,
        val fieldErrors: List<FieldErrorDetail> = emptyList(),
    ) : NetworkResult<Nothing>
    data object Loading : NetworkResult<Nothing>
}

data class FieldErrorDetail(val field: String, val message: String)

suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(block())
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Erreur inconnue")
    }
