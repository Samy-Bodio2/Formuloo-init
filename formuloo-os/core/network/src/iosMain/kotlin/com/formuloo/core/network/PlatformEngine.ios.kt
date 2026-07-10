package com.formuloo.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual fun platformEngineFactory(): HttpClientEngineFactory<*> = Darwin

internal actual val devBackendHost: String = "localhost"
