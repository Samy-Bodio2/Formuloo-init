package com.formuloo.feature.hr

import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Contract
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.EmployeeStatus
import com.formuloo.feature.hr.domain.model.EmployeeType
import com.formuloo.feature.hr.domain.model.Gender
import com.formuloo.feature.hr.domain.model.LeaveBalance
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.domain.model.OrgNode
import com.formuloo.feature.hr.domain.model.Payslip
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeListViewModelTest {

    // Scheduler partagé entre runTest et viewModelScope via Dispatchers.Main
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Test 1 : état initial = Loading ───────────────────────────────────

    @Test
    fun `initial state is Loading`() = runTest(testScheduler) {
        val vm = EmployeeListViewModel(FakeHrRepository())
        assertIs<UiState.Loading>(vm.uiState.value)
    }

    // ── Test 2 : après chargement → Success ───────────────────────────────

    @Test
    fun `after load completes state becomes Success with all employees`() = runTest(testScheduler) {
        val employees = listOf(
            fakeEmployee("1", "Alice", "Martin"),
            fakeEmployee("2", "Bob", "Dupont"),
        )
        val vm = EmployeeListViewModel(FakeHrRepository(employees = employees))

        advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<UiState.Success<*>>(state)
        assertEquals(2, (state as UiState.Success).data.size)
    }

    // ── Test 3 : search("Amadou") → filtre la liste ───────────────────────

    @Test
    fun `search filters employee list by first name`() = runTest(testScheduler) {
        val employees = listOf(
            fakeEmployee("1", "Amadou", "Diallo"),
            fakeEmployee("2", "Alice", "Martin"),
        )
        val vm = EmployeeListViewModel(FakeHrRepository(employees = employees))
        advanceUntilIdle()

        vm.search("Amadou")
        advanceUntilIdle()  // avance de 300 ms de temps virtuel pour le debounce

        val state = vm.uiState.value
        assertIs<UiState.Success<*>>(state)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("Amadou", list.first().firstName)
    }

    // ── Test 4 : filterByStatus("on_leave") → filtre par statut ──────────

    @Test
    fun `filterByStatus shows only employees with matching status`() = runTest(testScheduler) {
        val employees = listOf(
            fakeEmployee("1", "Alice", "Martin", status = EmployeeStatus.ACTIVE),
            fakeEmployee("2", "Bob", "Dupont", status = EmployeeStatus.ON_LEAVE),
        )
        val vm = EmployeeListViewModel(FakeHrRepository(employees = employees))
        advanceUntilIdle()

        vm.filterByStatus("on_leave")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertIs<UiState.Success<*>>(state)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals(EmployeeStatus.ON_LEAVE, list.first().status)
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private fun fakeEmployee(
        id: String,
        firstName: String,
        lastName: String,
        status: EmployeeStatus = EmployeeStatus.ACTIVE,
    ) = Employee(
        id = id,
        employeeNumber = "EMP-$id",
        firstName = firstName,
        lastName = lastName,
        email = "${firstName.lowercase()}@test.cm",
        phone = "+237600000000",
        photoUrl = null,
        department = null,
        position = null,
        managerName = null,
        hireDate = "2022-01-01",
        status = status,
        employeeType = EmployeeType.PERMANENT,
        gender = Gender.M,
        nationality = null,
        address = null,
        numeroCnps = null,
        situationFamiliale = null,
        nombreEnfants = 0,
    )
}

// ── Fake HrRepository ─────────────────────────────────────────────────────

class FakeHrRepository(
    private val employees: List<Employee> = emptyList(),
    private val networkError: Boolean = false,
) : HrRepository {

    override fun getEmployees(search: String?, status: String?): Flow<NetworkResult<List<Employee>>> = flow {
        emit(NetworkResult.Loading)
        if (networkError) {
            emit(NetworkResult.Error("Réseau indisponible"))
            return@flow
        }
        val filtered = employees.filter { e ->
            (search == null || e.firstName.contains(search, true) || e.lastName.contains(search, true)) &&
                    (status == null || e.status.name.lowercase() == status.lowercase())
        }
        emit(NetworkResult.Success(filtered))
    }

    override suspend fun getEmployee(id: String): NetworkResult<Employee> =
        employees.firstOrNull { it.id == id }
            ?.let { NetworkResult.Success(it) }
            ?: NetworkResult.Error("Introuvable", code = 404)

    override suspend fun createEmployee(
        firstName: String, lastName: String, gender: String, email: String,
        phone: String, hireDate: String, status: String, typeEmploye: String,
        birthDate: String?, nationality: String?, nationalId: String?,
        situationFamiliale: String?, nombreEnfants: Int, numeroCnps: String?,
        address: String?, departmentId: String?, positionId: String?,
        managerId: String?, photoUrl: String?,
    ): NetworkResult<Employee> = NetworkResult.Error("Non implémenté")

    override fun getContracts(employeeId: String): Flow<NetworkResult<List<Contract>>> = flow {
        emit(NetworkResult.Success(emptyList()))
    }

    override suspend fun createContract(
        employeId: String, type: String, startDate: String, endDate: String?,
        grossSalary: Double, currency: String, workHoursPerWeek: Int,
        trialPeriod: Int?, documentUrl: String?, signedAt: String?,
    ): NetworkResult<Contract> = NetworkResult.Error("Non implémenté")

    override suspend fun getPendingLeaves(): NetworkResult<List<LeaveRequest>> =
        NetworkResult.Success(emptyList())

    override suspend fun approveLeave(id: String, commentaire: String?): NetworkResult<LeaveRequest> =
        NetworkResult.Error("Non implémenté")

    override suspend fun rejectLeave(id: String, reason: String): NetworkResult<LeaveRequest> =
        NetworkResult.Error("Non implémenté")

    override fun getMyLeaves(): Flow<NetworkResult<List<LeaveRequest>>> = flow {
        emit(NetworkResult.Success(emptyList()))
    }

    override suspend fun getLeaveBalance(employeeId: String?, annee: Int?): NetworkResult<List<LeaveBalance>> =
        NetworkResult.Success(emptyList())

    override suspend fun requestLeave(
        typeCode: String, startDate: String, endDate: String, reason: String?,
    ): NetworkResult<LeaveRequest> = NetworkResult.Error("Non implémenté")

    override suspend fun cancelLeave(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)

    override fun getTeamPendingLeaves(): Flow<NetworkResult<List<LeaveRequest>>> = flow {
        emit(NetworkResult.Success(emptyList()))
    }

    override suspend fun approveLeave(id: String): NetworkResult<LeaveRequest> =
        NetworkResult.Error("Non implémenté")

    override fun getMyPayslips(annee: Int?): Flow<NetworkResult<List<Payslip>>> = flow {
        emit(NetworkResult.Success(emptyList()))
    }

    override suspend fun getPayslipDetail(id: String): NetworkResult<Payslip> =
        NetworkResult.Error("Non implémenté")

    override suspend fun getOrganizationTree(): NetworkResult<List<OrgNode>> =
        NetworkResult.Success(emptyList())
}
