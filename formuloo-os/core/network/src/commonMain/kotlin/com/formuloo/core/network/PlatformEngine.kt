package com.formuloo.core.network

import io.ktor.client.engine.HttpClientEngineFactory

internal expect fun platformEngineFactory(): HttpClientEngineFactory<*>

/**
 * Autorité "host:port" à utiliser pour atteindre le backend tournant sur la machine
 * de développement (gateway nginx exposé en 80 sur l'hôte).
 * Desktop (JVM) et simulateur iOS partagent le réseau de l'hôte : "localhost:80" fonctionne.
 * Android (émulateur ou appareil physique via `adb reverse tcp:8080 tcp:80`) utilise
 * "localhost:8080" — port 80 étant un port privilégié qu'adb ne peut pas rediriger
 * directement côté device sans root.
 */
internal expect val devBackendHost: String
