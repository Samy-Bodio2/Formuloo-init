package com.formuloo.core.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

internal actual fun platformEngineFactory(): HttpClientEngineFactory<*> = Android

// "localhost:8080" + `adb reverse tcp:8080 tcp:80` : fonctionne aussi bien sur
// appareil physique (USB) que sur émulateur, contrairement à 10.0.2.2 (magique,
// émulateur uniquement, injoignable depuis un vrai téléphone). Port 8080 côté
// device car adb ne peut pas binder un listener sur le port 80 (privilégié)
// sans root.
internal actual val devBackendHost: String = "localhost:8080"
