package com.formuloo.core.common.sync

import kotlinx.coroutines.flow.StateFlow

/** Observe l'état de connectivité réseau de l'appareil. */
interface NetworkObserver {
    val isOnline: StateFlow<Boolean>
}

/** Implémentation expect/actual par plateforme (ConnectivityManager / NWPathMonitor / sonde réseau). */
expect fun createNetworkObserver(): NetworkObserver
