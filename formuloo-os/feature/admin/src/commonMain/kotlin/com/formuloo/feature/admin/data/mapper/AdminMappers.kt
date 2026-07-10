package com.formuloo.feature.admin.data.mapper

import com.formuloo.core.network.dto.auth.RoleDto
import com.formuloo.core.network.dto.auth.UserListItemDto
import com.formuloo.feature.admin.domain.model.AdminUser
import com.formuloo.feature.admin.domain.model.ModuleOption
import com.formuloo.feature.admin.domain.model.RoleOption
import com.formuloo.feature.admin.domain.model.UserStatus

/**
 * Modules accessibles depuis le formulaire d'invitation. Seuls RH/Compta/CRM ont un rôle
 * système correspondant côté backend aujourd'hui (cf. migration 0009_seed_system_roles) —
 * Stock/Projets/Analytics sont affichés mais désactivés tant qu'aucune permission backend
 * ne les couvre.
 */
val MODULE_OPTIONS = listOf(
    ModuleOption("hr", "Ressources Humaines", supported = true),
    ModuleOption("compta", "Comptabilité", supported = true),
    ModuleOption("crm", "CRM & Ventes", supported = true),
    ModuleOption("stock", "Stock", supported = false),
    ModuleOption("projects", "Projets", supported = false),
    ModuleOption("analytics", "Analytics", supported = false),
)

private val ROLE_DESCRIPTIONS = mapOf(
    "SUPER_ADMIN" to "Accès total à la plateforme, toutes organisations.",
    "ADMIN_PME" to "Accès total à son organisation, tous les modules.",
    "RH_MANAGER" to "Gestion des employés, contrats, paie et congés.",
    "MANAGER" to "Gestion d'équipe, approbation des congés.",
    "COMPTABLE" to "Écritures, factures, paiements et états financiers.",
    "COMMERCIAL" to "CRM, devis, factures et pipeline.",
    "EMPLOYE" to "Accès à ses propres données uniquement.",
)

private val ROLE_TO_MODULES = mapOf(
    "SUPER_ADMIN" to listOf("hr", "compta", "crm", "stock", "projects", "analytics"),
    "ADMIN_PME" to listOf("hr", "compta", "crm", "stock", "projects", "analytics"),
    "RH_MANAGER" to listOf("hr"),
    "MANAGER" to listOf("hr", "analytics"),
    "COMPTABLE" to listOf("compta"),
    "COMMERCIAL" to listOf("crm"),
    "EMPLOYE" to emptyList(),
)

fun RoleDto.toOption(): RoleOption = RoleOption(
    code = code,
    name = name,
    description = ROLE_DESCRIPTIONS[code] ?: "Rôle personnalisé.",
    moduleKeys = ROLE_TO_MODULES[code] ?: emptyList(),
)

private fun deriveStatus(isActive: Boolean, isVerified: Boolean): UserStatus = when {
    !isActive && !isVerified -> UserStatus.INVITED
    isActive -> UserStatus.ACTIVE
    else -> UserStatus.SUSPENDED
}

private fun initialsOf(firstName: String, lastName: String): String =
    "${firstName.firstOrNull() ?: ' '}${lastName.firstOrNull() ?: ' '}".trim().uppercase().ifBlank { "?" }

fun UserListItemDto.toDomain(roleNamesByCode: Map<String, String>): AdminUser {
    val primaryRoleCode = roles.firstOrNull()
    val roleLabel = primaryRoleCode?.let { roleNamesByCode[it] ?: it } ?: "Sans rôle"
    val moduleCount = roles.flatMap { ROLE_TO_MODULES[it] ?: emptyList() }.distinct().size
    return AdminUser(
        id = id,
        fullName = "$firstName $lastName".trim(),
        initials = initialsOf(firstName, lastName),
        email = email,
        roleLabel = roleLabel,
        status = deriveStatus(isActive, isVerified),
        moduleCount = moduleCount,
        createdAt = createdAt,
    )
}
