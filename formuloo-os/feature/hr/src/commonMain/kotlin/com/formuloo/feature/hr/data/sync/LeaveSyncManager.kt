package com.formuloo.feature.hr.data.sync

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.sync.NetworkObserver
import com.formuloo.core.network.api.HrRemoteDataSource
import com.formuloo.core.network.dto.hr.CongeCreateDto
import com.formuloo.feature.hr.data.source.local.HrLocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Synchronise les demandes de congé créées hors-ligne (FOR16A26-988).
 *
 * Remplace WorkManager/FCM — adaptation KMP : observe [NetworkObserver.isOnline]
 * et déclenche une synchronisation à chaque retour en ligne, ainsi qu'une fois
 * au démarrage de l'app (au cas où des demandes étaient en attente avant fermeture).
 *
 * Une demande qui échoue 5 fois de suite n'est plus retentée automatiquement
 * (évite la boucle infinie) — elle reste visible côté UI avec son badge "en attente".
 */
class LeaveSyncManager(
    private val networkObserver: NetworkObserver,
    private val local: HrLocalDataSource,
    private val remote: HrRemoteDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        const val MAX_SYNC_ATTEMPTS = 5
    }

    fun start() {
        scope.launch { syncPendingLeaves() }
        scope.launch {
            networkObserver.isOnline.collect { online ->
                if (online) syncPendingLeaves()
            }
        }
    }

    suspend fun syncPendingLeaves() {
        local.getPendingSyncLeaves().forEach { entity ->
            if (entity.sync_attempts >= MAX_SYNC_ATTEMPTS) return@forEach

            val result = remote.createLeaveRequest(
                CongeCreateDto(
                    typeConge = entity.type_code,
                    startDate = entity.start_date,
                    endDate = entity.end_date,
                    reason = entity.reason,
                )
            )
            when (result) {
                is NetworkResult.Success -> local.replaceLocalIdWithServerId(
                    localId = entity.id,
                    serverId = result.data.id,
                )
                is NetworkResult.Error -> local.incrementLeaveSyncAttempts(entity.id)
                else -> {}
            }
        }
    }
}
