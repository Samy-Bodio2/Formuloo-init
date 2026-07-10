package com.formuloo.feature.hr.data.source.local

import com.formuloo.core.database.ContractEntity
import com.formuloo.core.database.EmployeeEntity
import com.formuloo.core.database.LeaveRequestEntity
import com.formuloo.core.database.PayslipEntity

interface HrLocalDataSource {

    // ── Employés ──────────────────────────────────────────────────────────

    fun getCachedEmployees(search: String?, status: String?): List<EmployeeEntity>

    fun getCachedEmployee(id: String): EmployeeEntity?

    /** Supprime tous les employés en cache puis insère [employees] dans une transaction. */
    fun replaceAllEmployees(employees: List<EmployeeEntity>)

    fun saveEmployee(employee: EmployeeEntity)

    // ── Contrats ──────────────────────────────────────────────────────────

    fun getCachedContracts(employeeId: String): List<ContractEntity>

    /** Supprime les contrats de [employeeId] puis insère [contracts] dans une transaction. */
    fun replaceContractsByEmployee(employeeId: String, contracts: List<ContractEntity>)

    fun saveContract(contract: ContractEntity)

    // ── Congés (FOR16A26-988 — file d'attente offline) ──────────────────────

    fun getCachedLeaves(): List<LeaveRequestEntity>

    fun getPendingSyncLeaves(): List<LeaveRequestEntity>

    /** Remplace les congés déjà synchronisés (is_pending_sync=0) sans toucher à la file d'attente offline. */
    fun replaceSyncedLeaves(leaves: List<LeaveRequestEntity>)

    fun saveLeave(leave: LeaveRequestEntity)

    fun replaceLocalIdWithServerId(localId: String, serverId: String)

    fun incrementLeaveSyncAttempts(id: String)

    fun deleteLeave(id: String)

    // ── Paie (lecture seule, cache simple) ───────────────────────────────

    fun getCachedPayslips(employeeId: String, annee: Int? = null): List<PayslipEntity>

    fun savePayslips(payslips: List<PayslipEntity>)
}
