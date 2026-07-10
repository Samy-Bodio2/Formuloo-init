package com.formuloo.feature.admin.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.admin.data.repository.AdminRepository
import com.formuloo.feature.admin.domain.model.AdminUser
import com.formuloo.feature.admin.domain.model.UserStats
import com.formuloo.feature.admin.domain.model.UserStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StatusFilter { ALL, ACTIVE, INVITED, SUSPENDED }

class UsersViewModel(private val repository: AdminRepository) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 10
    }

    private val _allUsers = MutableStateFlow<List<AdminUser>>(emptyList())
    private val _filteredUsers = MutableStateFlow<List<AdminUser>>(emptyList())

    private val _state = MutableStateFlow<UiState<List<AdminUser>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AdminUser>>> = _state.asStateFlow()

    private val _stats = MutableStateFlow(UserStats(0, 0, 0, 0))
    val stats: StateFlow<UserStats> = _stats.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalResults = MutableStateFlow(0)
    val totalResults: StateFlow<Int> = _totalResults.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            when (val result = repository.getUsers()) {
                is NetworkResult.Success -> {
                    _allUsers.value = result.data
                    _stats.value = computeStats(result.data)
                    applyFilters()
                }
                is NetworkResult.Error -> _state.value = UiState.Error(result.message, result.code)
                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
        applyFilters()
    }

    fun setPage(page: Int) {
        _currentPage.value = page.coerceIn(1, _totalPages.value)
        updatePagedState()
    }

    fun setUserActive(userId: String, active: Boolean) {
        viewModelScope.launch {
            when (repository.setUserActive(userId, active)) {
                is NetworkResult.Success -> loadUsers()
                else -> Unit
            }
        }
    }

    private fun applyFilters() {
        val query = _searchQuery.value.trim().lowercase()
        val filter = _statusFilter.value
        val filtered = _allUsers.value.filter { user ->
            val matchesQuery = query.isBlank() ||
                user.fullName.lowercase().contains(query) ||
                user.email.lowercase().contains(query)
            val matchesStatus = when (filter) {
                StatusFilter.ALL -> true
                StatusFilter.ACTIVE -> user.status == UserStatus.ACTIVE
                StatusFilter.INVITED -> user.status == UserStatus.INVITED
                StatusFilter.SUSPENDED -> user.status == UserStatus.SUSPENDED
            }
            matchesQuery && matchesStatus
        }
        _filteredUsers.value = filtered
        _totalResults.value = filtered.size
        _totalPages.value = if (filtered.isEmpty()) 1 else (filtered.size + PAGE_SIZE - 1) / PAGE_SIZE
        _currentPage.value = 1
        updatePagedState()
    }

    private fun updatePagedState() {
        val filtered = _filteredUsers.value
        val pageItems = filtered.drop((_currentPage.value - 1) * PAGE_SIZE).take(PAGE_SIZE)
        _state.value = if (filtered.isEmpty()) UiState.Empty else UiState.Success(pageItems)
    }

    private fun computeStats(users: List<AdminUser>): UserStats = UserStats(
        total = users.size,
        active = users.count { it.status == UserStatus.ACTIVE },
        invited = users.count { it.status == UserStatus.INVITED },
        suspended = users.count { it.status == UserStatus.SUSPENDED },
    )
}
