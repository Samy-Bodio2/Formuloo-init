package com.formuloo.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

internal actual fun platformEngineFactory(): HttpClientEngineFactory<*> = Android

internal actual val devBackendHost: String = "10.0.2.2"
