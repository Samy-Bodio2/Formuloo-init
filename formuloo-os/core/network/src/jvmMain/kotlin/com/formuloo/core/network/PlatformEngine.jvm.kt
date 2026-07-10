package com.formuloo.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java

internal actual fun platformEngineFactory(): HttpClientEngineFactory<*> = Java

internal actual val devBackendHost: String = "localhost:80"
