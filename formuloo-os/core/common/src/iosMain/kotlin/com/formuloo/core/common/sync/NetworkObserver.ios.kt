package com.formuloo.core.common.sync

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

actual fun createNetworkObserver(): NetworkObserver = IosNetworkObserver()

/**
 * Utilise NWPathMonitor (Network.framework) — pas de WorkManager/FCM ici, conforme
 * à l'adaptation KMP demandée. NOTE : non compilé/vérifié dans cette session (hôte Windows,
 * les cibles iOS nécessitent un toolchain macOS — voir TODO de fin de ticket).
 */
@OptIn(ExperimentalForeignApi::class)
private class IosNetworkObserver : NetworkObserver {

    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create("com.formuloo.network.monitor", null)

    init {
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_set_update_handler(monitor) { path ->
            _isOnline.value = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_start(monitor)
    }
}
