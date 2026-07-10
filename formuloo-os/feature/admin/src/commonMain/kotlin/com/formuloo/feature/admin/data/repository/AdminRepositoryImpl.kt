package com.formuloo.feature.admin.data.repository

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.network.dto.auth.AuthApiService
import com.formuloo.core.network.dto.auth.UserListItemDto
import com.formuloo.feature.admin.data.mapper.toDomain
import com.formuloo.feature.admin.data.mapper.toOption
import com.formuloo.feature.admin.domain.model.AdminUser
import com.formuloo.feature.admin.domain.model.RoleOption

class AdminRepositoryImpl(private val authApiService: AuthApiService) : AdminRepository {

    override suspend fun getUsers(): NetworkResult<List<AdminUser>> {
        val rolesResult = authApiService.getRoles()
        val roleNamesByCode = (rolesResult as? NetworkResult.Success)?.data
            ?.associate { it.code to it.name }
            ?: emptyMap()

        val allUsers = mutableListOf<UserListItemDto>()
        var page = 1
        while (true) {
            when (val result = authApiService.getUsersPage(page)) {
                is NetworkResult.Success -> {
                    allUsers += result.data.results
                    if (result.data.next == null) break
                    page += 1
                }
                is NetworkResult.Error -> return result
                is NetworkResult.Loading -> return NetworkResult.Loading
            }
        }
        return NetworkResult.Success(allUsers.map { it.toDomain(roleNamesByCode) })
    }

    override suspend fun getRoles(): NetworkResult<List<RoleOption>> =
        when (val result = authApiService.getRoles()) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toOption() })
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun inviteUser(
        firstName: String,
        lastName: String,
        email: String,
        roles: List<String>,
    ): NetworkResult<Unit> =
        when (val result = authApiService.inviteUser(firstName, lastName, email, roles)) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }

    override suspend fun setUserActive(userId: String, active: Boolean): NetworkResult<Unit> =
        when (val result = authApiService.setUserActive(userId, active)) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Error -> result
            is NetworkResult.Loading -> NetworkResult.Loading
        }
}
