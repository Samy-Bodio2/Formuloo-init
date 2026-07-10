package com.formuloo.core.network

import io.ktor.client.engine.HttpClientEngineFactory

internal expect fun platformEngineFactory(): HttpClientEngineFactory<*>

/**
 * Hôte à utiliser pour atteindre le backend tournant sur la machine de développement.
 * L'émulateur Android ne partage pas l'espace réseau de l'hôte : "localhost" y désigne
 * l'émulateur lui-même, pas la machine hôte — il faut l'alias spécial "10.0.2.2".
 * Desktop (JVM) et simulateur iOS partagent le réseau de l'hôte : "localhost" fonctionne.
 */
internal expect val devBackendHost: String
