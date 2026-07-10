package com.formuloo.feature.hr

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.sync.NetworkObserver
import com.formuloo.core.database.ContractEntity
import com.formuloo.core.database.EmployeeEntity
import com.formuloo.core.database.LeaveRequestEntity
import com.formuloo.core.database.PayslipEntity
import com.formuloo.core.network.api.HrRemoteDataSource
import com.formuloo.core.network.dto.hr.CongeApproveDto
import com.formuloo.core.network.dto.hr.CongeCreateDto
import com.formuloo.core.network.dto.hr.CongeDto
import com.formuloo.core.network.dto.hr.CongeRejectDto
import com.formuloo.core.network.dto.hr.CongeTypeDto
import com.formuloo.core.network.dto.hr.ContratCreateDto
import com.formuloo.core.network.dto.hr.ContratDto
import com.formuloo.core.network.dto.hr.DepartementTreeDto
import com.formuloo.core.network.dto.hr.EmployeBriefDto
import com.formuloo.core.network.dto.hr.EmployeCreateDto
import com.formuloo.core.network.dto.hr.EmployeDto
import com.formuloo.core.network.dto.hr.PaginatedResponse
import com.formuloo.core.network.dto.hr.PaieDto
import com.formuloo.core.network.dto.hr.SoldeCongesDto
import com.formuloo.feature.hr.data.repository.HrRepositoryImpl
import com.formuloo.feature.hr.data.source.local.HrLocalDataSource
import com.formuloo.feature.hr.domain.model.LeaveStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HrRepositoryTest {

    // ── Test 1 : getEmployees émet Loading → cache → données fraîches ──────

    @Test
    fun `getEmployees emits Loading then cached data then fresh data from network`() = runTest {
        val alice = fakeEmployeeEntity("1", "Alice", "Martin")
        val local = FakeHrLocalDataSource(employees = mutableListOf(alice))
        val remote = FakeHrRemoteDataSource(
            employees = listOf(
                fakeEmployeeDto("1", "Alice", "Martin"),
                fakeEmployeeDto("2", "Bob", "Dupont"),
            )
        )
        val repo = HrRepositoryImpl(remote, local, FakeNetworkObserver())

        val results = repo.getEmployees().toList()

        assertIs<NetworkResult.Loading>(results[0])
        assertIs<NetworkResult.Success<*>>(results[1])
        assertEquals(1, (results[1] as NetworkResult.Success).data.size)
        assertIs<NetworkResult.Success<*>>(results[2])
        assertEquals(2, (results[2] as NetworkResult.Success).data.size)
    }

    // ── Test 2 : cache vide + erreur réseau → émet Error ─────────────────

    @Test
    fun `getEmployees with empty cache and network error emits only Error`() = runTest {
        val local = FakeHrLocalDataSource()
        val remote = FakeHrRemoteDataSource(networkError = true)
        val repo = HrRepositoryImpl(remote, local, FakeNetworkObserver())

        val results = repo.getEmployees().toList()

        assertIs<NetworkResult.Loading>(results[0])
        assertEquals(2, results.size)
        assertIs<NetworkResult.Error>(results[1])
    }

    // ── Test 3 : getEmployee cache miss → API → sauvegarde en cache ───────

    @Test
    fun `getEmployee on cache miss fetches from network and saves locally`() = runTest {
        val local = FakeHrLocalDataSource()
        val dto = fakeEmployeeDto("42", "Moussa", "Keita")
        val remote = FakeHrRemoteDataSource(employees = listOf(dto))
        val repo = HrRepositoryImpl(remote, local, FakeNetworkObserver())

        val result = repo.getEmployee("42")

        assertIs<NetworkResult.Success<*>>(result)
        assertEquals("Moussa", (result as NetworkResult.Success).data.firstName)
        assertEquals(1, local.employees.size)
    }

    // ── Test 4 : approveLeave → retourne LeaveRequest.APPROVED ───────────

    @Test
    fun `approveLeave returns Success with APPROVED status`() = runTest {
        val local = FakeHrLocalDataSource()
        val remote = FakeHrRemoteDataSource()
        val repo = HrRepositoryImpl(remote, local, FakeNetworkObserver())

        val result = repo.approveLeave("leave-1", "Approuvé")

        assertIs<NetworkResult.Success<*>>(result)
        assertEquals(LeaveStatus.APPROVED, (result as NetworkResult.Success).data.status)
    }

    // ── Test 5 : requestLeave hors-ligne → insertion locale optimiste ────

    @Test
    fun `requestLeave while offline saves locally with pending sync flag`() = runTest {
        val local = FakeHrLocalDataSource()
        val remote = FakeHrRemoteDataSource()
        val repo = HrRepositoryImpl(remote, local, FakeNetworkObserver(online = false))

        val result = repo.requestLeave("annuel", "2025-07-01", "2025-07-10", "Vacances")

        assertIs<NetworkResult.Success<*>>(result)
        assertEquals(1, local.leaves.size)
        assertTrue(local.leaves.first().is_pending_sync == 1L)
        assertTrue(local.leaves.first().id.startsWith("local-"))
    }

    // ── Test 6 : requestLeave en ligne → envoi direct à l'API ─────────────

    @Test
    fun `requestLeave while online sends directly to the API`() = runTest {
        val local = FakeHrLocalDataSource()
        val remote = FakeHrRemoteDataSource()
        val repo = HrRepositoryImpl(remote, local, FakeNetworkObserver(online = true))

        val result = repo.requestLeave("annuel", "2025-07-01", "2025-07-10", "Vacances")

        assertIs<NetworkResult.Success<*>>(result)
        assertEquals(1, local.leaves.size)
        assertEquals(0L, local.leaves.first().is_pending_sync)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun fakeEmployeeDto(id: String, firstName: String, lastName: String) = EmployeDto(
        id = id,
        employeeNumber = "EMP-$id",
        firstName = firstName,
        lastName = lastName,
        gender = "M",
        phone = "+237600000000",
        email = "${firstName.lowercase()}@test.cm",
        hireDate = "2022-01-01",
        status = "active",
        typeEmploye = "permanent",
        createdAt = "2022-01-01T00:00:00Z",
        updatedAt = "2022-01-01T00:00:00Z",
    )

    private fun fakeEmployeeEntity(id: String, firstName: String, lastName: String) = EmployeeEntity(
        id = id,
        employee_number = "EMP-$id",
        first_name = firstName,
        last_name = lastName,
        email = "${firstName.lowercase()}@test.cm",
        phone = "+237600000000",
        photo_url = null,
        department_name = null,
        position_title = null,
        hire_date = "2022-01-01",
        status = "active",
        employee_type = "permanent",
        gender = "M",
        nationality = null,
        address = null,
        numero_cnps = null,
        situation_familiale = null,
        nombre_enfants = 0L,
        manager_name = null,
        updated_at = "2022-01-01T00:00:00Z",
    )
}

// ── Fakes ─────────────────────────────────────────────────────────────────

class FakeNetworkObserver(online: Boolean = true) : NetworkObserver {
    private val _isOnline = MutableStateFlow(online)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    fun setOnline(value: Boolean) { _isOnline.value = value }
}

class FakeHrRemoteDataSource(
    private val employees: List<EmployeDto> = emptyList(),
    private val networkError: Boolean = false,
) : HrRemoteDataSource {

    var lastCreatedLeave: CongeCreateDto? = null

    override suspend fun getEmployees(
        page: Int,
        pageSize: Int,
        search: String?,
        statut: String?,
        departementId: String?,
    ): NetworkResult<PaginatedResponse<EmployeDto>> {
        if (networkError) return NetworkResult.Error("Réseau indisponible")
        val filtered = employees.filter { e ->
            (search == null || e.firstName.contains(search, true) || e.lastName.contains(search, true)) &&
                    (statut == null || e.status == statut)
        }
        return NetworkResult.Success(PaginatedResponse(count = filtered.size, results = filtered))
    }

    override suspend fun getEmployee(id: String): NetworkResult<EmployeDto> {
        if (networkError) return NetworkResult.Error("Réseau indisponible")
        return employees.firstOrNull { it.id == id }
            ?.let { NetworkResult.Success(it) }
            ?: NetworkResult.Error("Employé introuvable", code = 404)
    }

    override suspend fun createEmployee(dto: EmployeCreateDto): NetworkResult<EmployeDto> =
        NetworkResult.Error("Non implémenté dans le fake")

    override suspend fun getContrats(
        employeId: String,
        isActive: Boolean?,
    ): NetworkResult<PaginatedResponse<ContratDto>> =
        NetworkResult.Success(PaginatedResponse(count = 0, results = emptyList()))

    override suspend fun createContrat(dto: ContratCreateDto): NetworkResult<ContratDto> =
        NetworkResult.Error("Non implémenté dans le fake")

    override suspend fun getLeaves(
        employeId: String?,
        statut: String?,
    ): NetworkResult<PaginatedResponse<CongeDto>> =
        NetworkResult.Success(PaginatedResponse(count = 0, results = emptyList()))

    override suspend fun approveLeave(id: String, dto: CongeApproveDto): NetworkResult<CongeDto> =
        NetworkResult.Success(fakeConge(id, "approved"))

    override suspend fun rejectLeave(id: String, dto: CongeRejectDto): NetworkResult<CongeDto> =
        NetworkResult.Success(fakeConge(id, "rejected"))

    override suspend fun createLeaveRequest(dto: CongeCreateDto): NetworkResult<CongeDto> {
        lastCreatedLeave = dto
        if (networkError) return NetworkResult.Error("Réseau indisponible")
        return NetworkResult.Success(fakeConge("server-1", "pending"))
    }

    override suspend fun cancelLeaveRequest(id: String): NetworkResult<Unit> =
        if (networkError) NetworkResult.Error("Réseau indisponible") else NetworkResult.Success(Unit)

    override suspend fun getLeaveBalance(
        employeId: String?,
        annee: Int?,
    ): NetworkResult<PaginatedResponse<SoldeCongesDto>> =
        NetworkResult.Success(PaginatedResponse(count = 0, results = emptyList()))

    override suspend fun getPayslips(
        employeId: String?,
        mois: Int?,
        annee: Int?,
        statut: String?,
    ): NetworkResult<PaginatedResponse<PaieDto>> =
        NetworkResult.Success(PaginatedResponse(count = 0, results = emptyList()))

    override suspend fun getPayslip(id: String): NetworkResult<PaieDto> =
        NetworkResult.Error("Non implémenté dans le fake")

    override suspend fun getOrganizationTree(): NetworkResult<List<DepartementTreeDto>> =
        NetworkResult.Success(emptyList())

    private fun fakeConge(id: String, status: String) = CongeDto(
        id = id,
        employee = EmployeBriefDto("emp-1", "EMP-001", "Alice", "Martin", "alice@test.cm"),
        type = CongeTypeDto(code = "annuel", libelle = "Congé annuel"),
        startDate = "2025-07-01",
        endDate = "2025-07-15",
        days = 10,
        status = status,
        createdAt = "2025-06-01T00:00:00Z",
    )
}

class FakeHrLocalDataSource(
    val employees: MutableList<EmployeeEntity> = mutableListOf(),
    val contracts: MutableList<ContractEntity> = mutableListOf(),
    val leaves: MutableList<LeaveRequestEntity> = mutableListOf(),
    val payslips: MutableList<PayslipEntity> = mutableListOf(),
) : HrLocalDataSource {

    override fun getCachedEmployees(search: String?, status: String?): List<EmployeeEntity> =
        employees.filter { e ->
            (search == null || e.first_name.contains(search, true) || e.last_name.contains(search, true)) &&
                    (status == null || e.status == status)
        }

    override fun getCachedEmployee(id: String): EmployeeEntity? =
        employees.firstOrNull { it.id == id }

    override fun replaceAllEmployees(newEmployees: List<EmployeeEntity>) {
        employees.clear()
        employees.addAll(newEmployees)
    }

    override fun saveEmployee(employee: EmployeeEntity) {
        val idx = employees.indexOfFirst { it.id == employee.id }
        if (idx >= 0) employees[idx] = employee else employees.add(employee)
    }

    override fun getCachedContracts(employeeId: String): List<ContractEntity> =
        contracts.filter { it.employee_id == employeeId }

    override fun replaceContractsByEmployee(employeeId: String, newContracts: List<ContractEntity>) {
        contracts.removeAll { it.employee_id == employeeId }
        contracts.addAll(newContracts)
    }

    override fun saveContract(contract: ContractEntity) {
        val idx = contracts.indexOfFirst { it.id == contract.id }
        if (idx >= 0) contracts[idx] = contract else contracts.add(contract)
    }

    override fun getCachedLeaves(): List<LeaveRequestEntity> = leaves.toList()

    override fun getPendingSyncLeaves(): List<LeaveRequestEntity> = leaves.filter { it.is_pending_sync == 1L }

    override fun replaceSyncedLeaves(newLeaves: List<LeaveRequestEntity>) {
        leaves.removeAll { it.is_pending_sync == 0L }
        leaves.addAll(newLeaves)
    }

    override fun saveLeave(leave: LeaveRequestEntity) {
        val idx = leaves.indexOfFirst { it.id == leave.id }
        if (idx >= 0) leaves[idx] = leave else leaves.add(leave)
    }

    override fun replaceLocalIdWithServerId(localId: String, serverId: String) {
        val idx = leaves.indexOfFirst { it.id == localId }
        if (idx >= 0) leaves[idx] = leaves[idx].copy(id = serverId, is_pending_sync = 0L)
    }

    override fun incrementLeaveSyncAttempts(id: String) {
        val idx = leaves.indexOfFirst { it.id == id }
        if (idx >= 0) leaves[idx] = leaves[idx].copy(sync_attempts = leaves[idx].sync_attempts + 1)
    }

    override fun deleteLeave(id: String) {
        leaves.removeAll { it.id == id }
    }

    override fun getCachedPayslips(employeeId: String, annee: Int?): List<PayslipEntity> =
        payslips.filter { it.employee_id == employeeId && (annee == null || it.annee == annee.toLong()) }

    override fun savePayslips(newPayslips: List<PayslipEntity>) {
        newPayslips.forEach { p ->
            val idx = payslips.indexOfFirst { it.id == p.id }
            if (idx >= 0) payslips[idx] = p else payslips.add(p)
        }
    }
}
