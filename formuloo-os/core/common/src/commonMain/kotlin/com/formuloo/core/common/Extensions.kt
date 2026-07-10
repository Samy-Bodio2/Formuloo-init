package com.formuloo.core.common

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data), code)
    is NetworkResult.Error -> NetworkResult.Error(message, code)
    is NetworkResult.Loading -> NetworkResult.Loading
}

inline fun <T> NetworkResult<T>.onSuccess(action: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (String, Int?) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) action(message, code)
    return this
}

fun <T> NetworkResult<T>.asUiState(): UiState<T> = when (this) {
    is NetworkResult.Success -> UiState.Success(data)
    is NetworkResult.Error -> UiState.Error(message, code)
    is NetworkResult.Loading -> UiState.Loading
}
