package com.formuloo.feature.hr.data.source.local

import com.formuloo.core.database.ContractEntity
import com.formuloo.core.database.EmployeeEntity
import com.formuloo.core.database.FormulooDatabase
import com.formuloo.core.database.LeaveRequestEntity
import com.formuloo.core.database.PayslipEntity

class SqlDelightHrLocalDataSource(private val db: FormulooDatabase) : HrLocalDataSource {

    // ── Employés ──────────────────────────────────────────────────────────

    override fun getCachedEmployees(search: String?, status: String?): List<EmployeeEntity> =
        when {
            !search.isNullOrBlank() -> db.employeeEntityQueries.search("%$search%").executeAsList()
            status != null -> db.employeeEntityQueries.filterByStatus(status).executeAsList()
            else -> db.employeeEntityQueries.getAll().executeAsList()
        }

    override fun getCachedEmployee(id: String): EmployeeEntity? =
        db.employeeEntityQueries.getById(id).executeAsOneOrNull()

    override fun replaceAllEmployees(employees: List<EmployeeEntity>) {
        db.transaction {
            db.employeeEntityQueries.deleteAll()
            employees.forEach { upsertEmployee(it) }
        }
    }

    override fun saveEmployee(employee: EmployeeEntity) {
        upsertEmployee(employee)
    }

    private fun upsertEmployee(e: EmployeeEntity) {
        db.employeeEntityQueries.upsert(
            e.id, e.employee_number, e.first_name, e.last_name, e.email,
            e.phone, e.photo_url, e.department_name, e.position_title,
            e.hire_date, e.status, e.employee_type, e.gender,
            e.nationality, e.address, e.numero_cnps, e.situation_familiale,
            e.nombre_enfants, e.manager_name, e.updated_at,
        )
    }

    // ── Contrats ──────────────────────────────────────────────────────────

    override fun getCachedContracts(employeeId: String): List<ContractEntity> =
        db.contractEntityQueries.getByEmployeeId(employeeId).executeAsList()

    override fun replaceContractsByEmployee(employeeId: String, contracts: List<ContractEntity>) {
        db.transaction {
            db.contractEntityQueries.deleteByEmployeeId(employeeId)
            contracts.forEach { upsertContract(it) }
        }
    }

    override fun saveContract(contract: ContractEntity) {
        upsertContract(contract)
    }

    private fun upsertContract(c: ContractEntity) {
        db.contractEntityQueries.upsert(
            c.id, c.numero, c.employee_id, c.employee_name, c.type,
            c.start_date, c.end_date, c.gross_salary, c.currency,
            c.work_hours_per_week, c.trial_period_days, c.is_active,
            c.document_url, c.signed_at, c.updated_at,
        )
    }

    // ── Congés ────────────────────────────────────────────────────────────

    override fun getCachedLeaves(): List<LeaveRequestEntity> =
        db.leaveRequestEntityQueries.getAll().executeAsList()

    override fun getPendingSyncLeaves(): List<LeaveRequestEntity> =
        db.leaveRequestEntityQueries.getPendingSync().executeAsList()

    override fun replaceSyncedLeaves(leaves: List<LeaveRequestEntity>) {
        db.transaction {
            db.leaveRequestEntityQueries.deleteAllSynced()
            leaves.forEach { saveLeave(it) }
        }
    }

    override fun saveLeave(leave: LeaveRequestEntity) {
        db.leaveRequestEntityQueries.upsert(
            leave.id, leave.employee_id, leave.employee_name, leave.employee_initials,
            leave.type_code, leave.type_libelle, leave.start_date, leave.end_date,
            leave.days, leave.reason, leave.status, leave.approved_by_name,
            leave.approved_at, leave.is_pending_sync, leave.sync_attempts, leave.created_at,
        )
    }

    override fun replaceLocalIdWithServerId(localId: String, serverId: String) {
        db.leaveRequestEntityQueries.replaceLocalIdWithServerId(serverId = serverId, localId = localId)
    }

    override fun incrementLeaveSyncAttempts(id: String) {
        db.leaveRequestEntityQueries.incrementSyncAttempts(id)
    }

    override fun deleteLeave(id: String) {
        db.leaveRequestEntityQueries.deleteById(id)
    }

    // ── Paie ──────────────────────────────────────────────────────────────

    override fun getCachedPayslips(employeeId: String, annee: Int?): List<PayslipEntity> =
        if (annee != null) {
            db.payslipEntityQueries.getByEmployeeIdAndYear(employeeId, annee.toLong()).executeAsList()
        } else {
            db.payslipEntityQueries.getByEmployeeId(employeeId).executeAsList()
        }

    override fun savePayslips(payslips: List<PayslipEntity>) {
        db.transaction {
            payslips.forEach { p ->
                db.payslipEntityQueries.upsert(
                    p.id, p.employee_id, p.period, p.mois, p.annee, p.gross,
                    p.prime_transport, p.prime_logement, p.prime_rendement, p.autres_primes,
                    p.cotisation_cnps, p.impot_irpp, p.credit_logement, p.autres_deductions,
                    p.net_salary, p.currency, p.status, p.paid_at, p.pdf_url, p.cached_at,
                )
            }
        }
    }
}
