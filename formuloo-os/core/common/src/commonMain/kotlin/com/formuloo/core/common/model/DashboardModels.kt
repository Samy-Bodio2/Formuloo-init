package com.formuloo.core.common.model

data class KpiCard(
    val label: String,
    val value: String,          // ex: "42", "18,24 M"
    val trend: String,          // ex: "+3", "+2,1 %", "+26,6 %"
    val trendPositive: Boolean, // true = vert, false = rouge
    val iconType: KpiIconType
)

enum class KpiIconType { PEOPLE, MONEY, CHART, TARGET }

data class PendingRequest(
    val id: String,
    val employeeName: String,
    val initials: String,       // ex: "IN"
    val type: String,           // ex: "Congés payés"
    val duration: String        // ex: "4 j"
)

data class ActivityItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeLabel: String,      // ex: "2 h", "Hier", "3 juin"
    val iconType: ActivityIconType
)

enum class ActivityIconType { PAYROLL, OPPORTUNITY, INVOICE, EMPLOYEE, DEFAULT }

data class ModuleItem(
    val id: String,
    val label: String,
    val subtitle: String,       // ex: "42 employés", "SYSCOHADA"
    val iconType: ModuleIconType,
    val badgeCount: Int = 0
)

enum class ModuleIconType { HR, ACCOUNTING, CRM, STOCK, PROJECTS, ANALYTICS, SETTINGS, DASHBOARD, DOCUMENTS }

data class CompanyInfo(
    val initials: String,       // ex: "SD"
    val name: String,           // ex: "Sahel Distribution"
    val type: String,           // ex: "Enterprise"
    val userCount: Int,
    val country: String
)

data class DashboardState(
    val userName: String,
    val userInitials: String,
    val greeting: String,       // "Bonjour" calculé selon l'heure
    val dateLabel: String,      // "Jeudi 5 juin 2025 · Santé du jour"
    val company: CompanyInfo,
    val kpis: List<KpiCard>,
    val pendingRequests: List<PendingRequest>,
    val recentActivity: List<ActivityItem>,
    val modules: List<ModuleItem>,
    val notificationCount: Int
)
