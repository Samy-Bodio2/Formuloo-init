package com.formuloo.core.common.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Détient le Context applicatif Android nécessaire à [ConnectivityManager].
 * Doit être initialisé une fois au démarrage — voir `FormulooApplication.onCreate()` —
 * avant tout appel à [createNetworkObserver], pour garder une factory expect/actual
 * sans paramètre (cohérent sur les 3 plateformes) plutôt que de coupler ce module à Koin.
 */
object AndroidAppContextHolder {
    lateinit var appContext: Context
}

actual fun createNetworkObserver(): NetworkObserver = AndroidNetworkObserver(AndroidAppContextHolder.appContext)

private class AndroidNetworkObserver(context: Context) : NetworkObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(currentlyOnline())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            _isOnline.value = currentlyOnline()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _isOnline.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
