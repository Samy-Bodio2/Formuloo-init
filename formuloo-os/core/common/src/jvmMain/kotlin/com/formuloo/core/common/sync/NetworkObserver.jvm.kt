package com.formuloo.core.common.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress

actual fun createNetworkObserver(): NetworkObserver = JvmNetworkObserver()

/** Desktop n'a pas d'API de connectivité standard — on sonde périodiquement (toutes les 10 s). */
private class JvmNetworkObserver : NetworkObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                _isOnline.value = ping()
                delay(10_000)
            }
        }
    }

    private fun ping(): Boolean = try {
        InetAddress.getByName("8.8.8.8").isReachable(3_000)
    } catch (e: Exception) {
        false
    }
}
