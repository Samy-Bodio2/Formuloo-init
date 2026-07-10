package com.formuloo.core.navigation

import kotlinx.serialization.Serializable

/**
 * Destinations de navigation typees, partagees entre composeApp et les
 * modules feature. Chaque route est serialisable pour l'API typesafe de
 * Navigation Compose (`NavHost.composable<Route.XXX>`).
 */
sealed interface Route {
    @Serializable
    data object Splash : Route

    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Login : Route

    @Serializable
    data class Otp(val email: String) : Route

    @Serializable
    data object AccountType : Route

    @Serializable
    data class Registration(val type: String) : Route

    @Serializable
    data object PasswordResetRequest : Route

    @Serializable
    data object PasswordResetConfirm : Route

    @Serializable
    data object InviteTokenEntry : Route

    @Serializable
    data class InvitationFlow(val code: String) : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Hr : Route

    @Serializable
    data class HrEmployee(val id: String) : Route

    @Serializable
    data class HrEmployeeEdit(val id: String) : Route

    @Serializable
    data class Payslip(val id: String) : Route

    @Serializable
    data object Compta : Route

    @Serializable
    data class Invoice(val id: Int) : Route

    @Serializable
    data class PurchaseInvoice(val id: Int) : Route

    @Serializable
    data object Crm : Route

    @Serializable
    data object Stock : Route

    @Serializable
    data object Projects : Route

    @Serializable
    data object Analytics : Route

    @Serializable
    data object AdminUsers : Route

    @Serializable
    data object GesDoc : Route

    @Serializable
    data object GesDocUpload : Route

    @Serializable
    data object GesDocAudit : Route

    @Serializable
    data class GesDocExtraction(val id: String) : Route

    @Serializable
    data class GesDocValidation(val id: String) : Route

    @Serializable
    data class GesDocCertification(val id: String) : Route

    @Serializable
    data class GesDocDetail(val id: String) : Route

    @Serializable
    data class GesDocOriginal(val id: String) : Route

    @Serializable
    data class HrContractCreate(val employeeId: String) : Route

    @Serializable
    data class HrLeaveRequest(val employeeName: String = "") : Route

    @Serializable
    data class HrPayslipDetail(
        val id: String,
        val employeeName: String = "",
        val employeeNumber: String = "",
        val employeePosition: String = "",
    ) : Route
}
