package com.formuloo.feature.admin.data.repository

import com.formuloo.core.common.NetworkResult
import com.formuloo.feature.admin.domain.model.AdminUser
import com.formuloo.feature.admin.domain.model.RoleOption

interface AdminRepository {
    suspend fun getUsers(): NetworkResult<List<AdminUser>>
    suspend fun getRoles(): NetworkResult<List<RoleOption>>
    suspend fun inviteUser(firstName: String, lastName: String, email: String, roles: List<String>): NetworkResult<Unit>
    suspend fun setUserActive(userId: String, active: Boolean): NetworkResult<Unit>
}
