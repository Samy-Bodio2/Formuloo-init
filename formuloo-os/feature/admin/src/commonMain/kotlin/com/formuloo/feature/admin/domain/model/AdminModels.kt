package com.formuloo.feature.admin.domain.model

enum class UserStatus { ACTIVE, INVITED, SUSPENDED }

data class AdminUser(
    val id: String,
    val fullName: String,
    val initials: String,
    val email: String,
    val roleLabel: String,
    val status: UserStatus,
    val moduleCount: Int,
    val createdAt: String?,
)

data class UserStats(
    val total: Int,
    val active: Int,
    val invited: Int,
    val suspended: Int,
)

/** Module accessible depuis le formulaire d'invitation. */
data class ModuleOption(
    val key: String,
    val label: String,
    /** false si aucun rôle/permission backend ne couvre encore ce module. */
    val supported: Boolean,
)

data class RoleOption(
    val code: String,
    val name: String,
    val description: String,
    /** Clés de [ModuleOption] couvertes par ce rôle — utilisées pour pré-cocher les modules. */
    val moduleKeys: List<String>,
)
