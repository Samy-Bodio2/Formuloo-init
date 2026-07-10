package com.formuloo.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.auth.domain.model.UserProfile
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.model.ActivityIconType
import com.formuloo.core.common.model.ActivityItem
import com.formuloo.core.common.model.CompanyInfo
import com.formuloo.core.common.model.DashboardState
import com.formuloo.core.common.model.KpiCard
import com.formuloo.core.common.model.KpiIconType
import com.formuloo.core.common.model.ModuleIconType
import com.formuloo.core.common.model.ModuleItem
import com.formuloo.core.common.model.PendingRequest
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.EmployeeStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class DashboardViewModel(
    private val hrRepo: HrRepository,
    private val comptaRepo: ComptaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(buildSkeleton())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun setUserProfile(profile: UserProfile) {
        val initials = buildString {
            profile.firstName.firstOrNull()?.uppercaseChar()?.let { append(it) }
            profile.lastName.firstOrNull()?.uppercaseChar()?.let { append(it) }
        }
        _state.update { it.copy(userName = profile.firstName, userInitials = initials.ifBlank { "?" }) }
    }

    private fun loadDashboard() {
        val (year, month) = currentMonthYear()
        val moisLabel = MOIS_FR[month]

        // Employés (offline-first flow — émet cache puis refresh)
        viewModelScope.launch {
            hrRepo.getEmployees().collect { result ->
                if (result is NetworkResult.Success) {
                    val actifs = result.data.count { it.status == EmployeeStatus.ACTIVE }
                    _state.update { s ->
                        val kpis = s.kpis.toMutableList()
                        kpis[0] = KpiCard("Effectif actif", "$actifs", "actifs", true, KpiIconType.PEOPLE)
                        s.copy(
                            kpis = kpis,
                            modules = s.modules.map {
                                if (it.id == "hr") it.copy(subtitle = "$actifs employés") else it
                            },
                        )
                    }
                }
            }
        }

        // Congés en attente (one-shot API)
        viewModelScope.launch {
            when (val result = hrRepo.getPendingLeaves()) {
                is NetworkResult.Success -> {
                    val list = result.data
                    _state.update { s ->
                        s.copy(
                            pendingRequests = list.take(5).map { leave ->
                                PendingRequest(
                                    id = leave.id,
                                    employeeName = leave.employeeName,
                                    initials = leave.employeeInitials.ifBlank {
                                        leave.employeeName
                                            .split(" ")
                                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                            .take(2)
                                            .joinToString("")
                                    },
                                    type = leave.leaveTypeLabel,
                                    duration = "${leave.days} j",
                                )
                            },
                            notificationCount = list.size,
                            modules = s.modules.map {
                                if (it.id == "hr") it.copy(badgeCount = list.size) else it
                            },
                        )
                    }
                }
                else -> {}
            }
        }

        // Paie du mois courant (offline-first flow)
        viewModelScope.launch {
            hrRepo.getMyPayslips().collect { result ->
                if (result is NetworkResult.Success) {
                    val slips = result.data.filter { it.mois == month && it.annee == year }
                    if (slips.isNotEmpty()) {
                        val masse = slips.sumOf { it.gross }
                        val paieItem = ActivityItem(
                            id = "payroll-$month-$year",
                            title = "Paie de $moisLabel générée",
                            subtitle = "${slips.size} fiches · ${formatCompact(masse)} FCFA",
                            timeLabel = moisLabel,
                            iconType = ActivityIconType.PAYROLL,
                        )
                        _state.update { s ->
                            val kpis = s.kpis.toMutableList()
                            kpis[1] = KpiCard(
                                label = "Masse salariale",
                                value = formatCompact(masse),
                                trend = "${slips.size} fiches",
                                trendPositive = true,
                                iconType = KpiIconType.MONEY,
                            )
                            val activity = listOf(paieItem) +
                                s.recentActivity.filter { it.id != paieItem.id }.take(4)
                            s.copy(kpis = kpis, recentActivity = activity)
                        }
                    }
                }
            }
        }

        // Stats Compta (one-shot API)
        viewModelScope.launch {
            when (val result = comptaRepo.getStats()) {
                is NetworkResult.Success -> {
                    val stats = result.data
                    val invoiceItem = if (stats.nbImpayeesEnRetard > 0) {
                        ActivityItem(
                            id = "invoices-overdue",
                            title = "${stats.nbImpayeesEnRetard} factures en retard",
                            subtitle = "${formatCompact(stats.montantImpayeEnRetard)} FCFA à encaisser",
                            timeLabel = "Aujourd'hui",
                            iconType = ActivityIconType.INVOICE,
                        )
                    } else null
                    _state.update { s ->
                        val kpis = s.kpis.toMutableList()
                        kpis[2] = KpiCard(
                            label = "CA · $moisLabel",
                            value = formatCompact(stats.caMois),
                            trend = "${stats.nbFacturesMois} factures",
                            trendPositive = true,
                            iconType = KpiIconType.CHART,
                        )
                        val activity = buildList {
                            addAll(s.recentActivity.filter { it.id != "invoices-overdue" })
                            invoiceItem?.let { add(it) }
                        }.take(5)
                        s.copy(
                            kpis = kpis,
                            recentActivity = activity,
                            modules = s.modules.map {
                                if (it.id == "accounting") it.copy(
                                    subtitle = "${stats.nbFacturesMois} factures · $moisLabel",
                                    badgeCount = stats.nbImpayeesEnRetard,
                                ) else it
                            },
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun onApproveRequest(id: String) {
        viewModelScope.launch {
            hrRepo.approveLeave(id)
            reloadPendingLeaves()
        }
    }

    fun onRejectRequest(id: String) {
        viewModelScope.launch {
            hrRepo.rejectLeave(id, "Refusé depuis le dashboard")
            reloadPendingLeaves()
        }
    }

    private suspend fun reloadPendingLeaves() {
        when (val result = hrRepo.getPendingLeaves()) {
            is NetworkResult.Success -> {
                val list = result.data
                _state.update { s ->
                    s.copy(
                        pendingRequests = list.take(5).map { leave ->
                            PendingRequest(
                                id = leave.id,
                                employeeName = leave.employeeName,
                                initials = leave.employeeInitials.ifBlank {
                                    leave.employeeName
                                        .split(" ")
                                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                        .take(2)
                                        .joinToString("")
                                },
                                type = leave.leaveTypeLabel,
                                duration = "${leave.days} j",
                            )
                        },
                        notificationCount = list.size,
                    )
                }
            }
            else -> {}
        }
    }

    fun onModuleClick(moduleId: String) {
        // navigation gérée par HomeScreen / App.kt
    }
}

// ── Helpers top-level ─────────────────────────────────────────────────────

private val MOIS_FR = arrayOf(
    "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre",
)

private fun currentMonthYear(): Pair<Int, Int> {
    var days = (Clock.System.now().toEpochMilliseconds() / 86_400_000L).toInt()
    var year = 1970
    while (true) {
        val yDays = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
        if (days < yDays) break
        days -= yDays
        year++
    }
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val monthDays = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (md in monthDays) {
        if (days < md) break
        days -= md
        month++
    }
    return Pair(year, month)
}

private fun computeGreeting(): String {
    val hour = (Clock.System.now().toEpochMilliseconds() / 3_600_000L) % 24
    return if (hour in 5..17) "Bonjour" else "Bonsoir"
}

private fun formatCompact(value: Double): String {
    return when {
        value >= 1_000_000_000 -> "${formatDec(value / 1_000_000_000.0)} Md"
        value >= 1_000_000 -> "${formatDec(value / 1_000_000.0)} M"
        value >= 1_000 -> "${(value / 1_000).toLong()} k"
        else -> value.toLong().toString()
    }
}

private fun formatDec(v: Double): String {
    val int = v.toLong()
    val dec2 = ((v - int) * 100).toLong()
    return when {
        dec2 == 0L -> "$int"
        dec2 % 10 == 0L -> "$int,${dec2 / 10}"
        else -> "$int,$dec2"
    }
}

private fun buildSkeleton(): DashboardState {
    val (_, month) = currentMonthYear()
    val moisLabel = MOIS_FR[month]
    return DashboardState(
        userName = "…",
        userInitials = "…",
        greeting = computeGreeting(),
        dateLabel = "$moisLabel ${currentMonthYear().first}",
        company = CompanyInfo("SD", "Sahel Distribution", "Enterprise", 0, "Cameroun"),
        kpis = listOf(
            KpiCard("Effectif actif", "—", "chargement", true, KpiIconType.PEOPLE),
            KpiCard("Masse salariale", "—", "chargement", true, KpiIconType.MONEY),
            KpiCard("CA · $moisLabel", "—", "chargement", true, KpiIconType.CHART),
            KpiCard("Pipeline actif", "—", "chargement", true, KpiIconType.TARGET),
        ),
        pendingRequests = emptyList(),
        recentActivity = emptyList(),
        modules = listOf(
            ModuleItem("hr", "RH", "—", ModuleIconType.HR),
            ModuleItem("accounting", "Comptabilité", "SYSCOHADA", ModuleIconType.ACCOUNTING),
            ModuleItem("gesdoc", "GED", "OCR · Blockchain", ModuleIconType.DOCUMENTS),
            ModuleItem("crm", "CRM", "9 opportunités", ModuleIconType.CRM, 9),
            ModuleItem("stock", "Stock", "Inventaire", ModuleIconType.STOCK),
            ModuleItem("projects", "Projets", "5 actifs", ModuleIconType.PROJECTS),
            ModuleItem("analytics", "Analytics", "12 rapports", ModuleIconType.ANALYTICS),
        ),
        notificationCount = 0,
    )
}
