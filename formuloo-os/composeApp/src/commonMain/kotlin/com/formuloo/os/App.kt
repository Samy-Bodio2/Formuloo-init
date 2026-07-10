package com.formuloo.os

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooTextPrimary
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.formuloo.core.auth.domain.model.UserProfile
import com.formuloo.core.designsystem.FormulooTheme
import com.formuloo.core.navigation.AppNavHost
import com.formuloo.core.navigation.Route
import com.formuloo.core.navigation.rememberAppNavController
import com.formuloo.feature.admin.presentation.screen.InviteUserDialog
import com.formuloo.feature.admin.presentation.screen.UsersScreen
import com.formuloo.feature.admin.presentation.viewmodel.UsersViewModel
import com.formuloo.feature.auth.AccountType
import com.formuloo.feature.auth.AccountTypeScreen
import com.formuloo.feature.auth.AuthViewModel
import com.formuloo.feature.auth.InvitationFlowScreen
import com.formuloo.feature.auth.InviteTokenEntryScreen
import com.formuloo.feature.auth.LoginScreen
import com.formuloo.feature.auth.OnboardingPreferences
import com.formuloo.feature.auth.OnboardingScreen
import com.formuloo.feature.auth.OtpVerificationScreen
import com.formuloo.feature.auth.PasswordResetConfirmScreen
import com.formuloo.feature.auth.PasswordResetRequestScreen
import com.formuloo.feature.auth.RegistrationScreen
import com.formuloo.feature.compta.presentation.screen.ComptaDashboardScreen
import com.formuloo.feature.gesdoc.presentation.screen.DocumentDetailScreen
import com.formuloo.feature.gesdoc.presentation.screen.DocumentOriginalScreen
import com.formuloo.feature.gesdoc.presentation.screen.GesDocAuditScreen
import com.formuloo.feature.gesdoc.presentation.screen.GesDocCertificationScreen
import com.formuloo.feature.gesdoc.presentation.screen.GesDocDashboardScreen
import com.formuloo.feature.gesdoc.presentation.screen.GesDocExtractionScreen
import com.formuloo.feature.gesdoc.presentation.screen.GesDocUploadScreen
import com.formuloo.feature.gesdoc.presentation.screen.GesDocValidationScreen
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocUploadViewModel
import com.formuloo.feature.compta.presentation.screen.InvoiceCreateScreen
import com.formuloo.feature.compta.presentation.screen.InvoiceDetailScreen
import com.formuloo.feature.compta.presentation.screen.InvoiceListScreen
import com.formuloo.feature.compta.presentation.screen.PaymentListScreen
import com.formuloo.feature.compta.presentation.screen.SupplierPaymentListScreen
import com.formuloo.feature.compta.presentation.screen.PurchaseInvoiceDetailScreen
import com.formuloo.feature.compta.presentation.screen.PurchaseInvoiceListScreen
import com.formuloo.feature.dashboard.presentation.HomeScreen
import com.formuloo.feature.hr.data.sync.LeaveSyncManager
import com.formuloo.feature.hr.presentation.screen.ContractCreateScreen
import com.formuloo.feature.hr.presentation.screen.EmployeeCreateScreen
import com.formuloo.feature.hr.presentation.screen.EmployeeDetailScreen
import com.formuloo.feature.hr.presentation.screen.EmployeeEditScreen
import com.formuloo.feature.hr.presentation.screen.EmployeeListScreen
import com.formuloo.feature.hr.presentation.screen.HrDashboardScreen
import com.formuloo.feature.hr.presentation.screen.LeaveRequestScreen
import com.formuloo.feature.hr.presentation.screen.hasHrManagerAccess
import com.formuloo.feature.hr.presentation.screen.hasPayslipAccess
import com.formuloo.feature.hr.presentation.screen.MyLeavesScreen
import com.formuloo.feature.hr.presentation.screen.MyPresencesScreen
import com.formuloo.feature.hr.presentation.screen.MesDemandesDocumentScreen
import com.formuloo.feature.hr.presentation.screen.OrgChartScreen
import com.formuloo.feature.hr.presentation.screen.DemandesDocumentRHScreen
import com.formuloo.feature.hr.presentation.screen.PayrollAdminScreen
import com.formuloo.feature.hr.presentation.screen.PresencesAdminScreen
import com.formuloo.feature.hr.presentation.screen.SoldesCongesAdminScreen
import com.formuloo.feature.hr.presentation.screen.StatsRHScreen
import com.formuloo.feature.hr.presentation.screen.PayslipDetailScreen
import com.formuloo.feature.hr.presentation.screen.PayslipListScreen
import com.formuloo.feature.hr.presentation.screen.TeamApprovalScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Profil de demonstration utilise tant qu'aucune session reelle n'est active,
 * pour visualiser le Dashboard sans backend disponible.
 * TODO: retirer une fois l'authentification reelle branchee de bout en bout.
 */
private val DemoUserProfile = UserProfile(
    id = "demo",
    email = "aicha.bamba@saheldistribution.com",
    firstName = "Aïcha",
    lastName = "Bamba",
    roles = listOf("admin"),
    isVerified = true,
)

@Composable
fun App() {
    FormulooTheme {
        val navController = rememberAppNavController()
        val leaveSyncManager: LeaveSyncManager = koinInject()
        LaunchedEffect(Unit) { leaveSyncManager.start() }

        AppNavHost(
            navController = navController,
            startDestination = Route.Splash,
        ) {
            composable<Route.Splash> {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Route.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Route.Onboarding) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Route.Login) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.Onboarding> {
                OnboardingScreen(
                    onOnboardingComplete = {
                        OnboardingPreferences.setOnboardingDone()
                        navController.navigate(Route.Login) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.Login> {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Route.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onOtpRequired = { email ->
                        navController.navigate(Route.Otp(email))
                    },
                    onNavigateToRegister = {
                        navController.navigate(Route.AccountType)
                    },
                    onForgotPassword = {
                        navController.navigate(Route.PasswordResetRequest)
                    },
                    onNavigateToInvite = {
                        navController.navigate(Route.InviteTokenEntry)
                    },
                )
            }
            composable<Route.PasswordResetRequest> {
                PasswordResetRequestScreen(
                    onBack = { navController.popBackStack() },
                    onLinkSent = { navController.navigate(Route.PasswordResetConfirm) },
                )
            }
            composable<Route.PasswordResetConfirm> {
                PasswordResetConfirmScreen(
                    onBack = { navController.popBackStack() },
                    onResetSuccess = {
                        navController.popBackStack(Route.Login, inclusive = false)
                    },
                )
            }
            composable<Route.InviteTokenEntry> {
                InviteTokenEntryScreen(
                    onBack = { navController.popBackStack() },
                    onCodeSubmitted = { code ->
                        navController.navigate(Route.InvitationFlow(code))
                    },
                )
            }
            composable<Route.InvitationFlow> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.InvitationFlow>()
                InvitationFlowScreen(
                    code = route.code,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(Route.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.Otp> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Otp>()
                OtpVerificationScreen(
                    email = route.email,
                    onVerified = {
                        navController.navigate(Route.Home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.AccountType> {
                AccountTypeScreen(
                    onBack = { navController.popBackStack() },
                    onContinue = { accountType ->
                        navController.navigate(Route.Registration(accountType.value))
                    },
                )
            }
            composable<Route.Registration> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Registration>()
                val accountType = AccountType.entries.first { it.value == route.type }
                RegistrationScreen(
                    accountType = accountType,
                    onBack = { navController.popBackStack() },
                    onRegistrationComplete = {
                        navController.popBackStack(Route.Login, inclusive = false)
                    },
                )
            }
            composable<Route.Home> {
                val authViewModel: AuthViewModel = koinViewModel()
                val userProfile by authViewModel.currentUser.collectAsStateWithLifecycle()
                HomeScreen(
                    userProfile = userProfile ?: DemoUserProfile,
                    onNavigateToModule = { moduleKey ->
                        if (moduleKey == "RH" || moduleKey == "hr") {
                            navController.navigate(Route.Hr)
                        } else if (moduleKey == "accounting" || moduleKey == "compta") {
                            navController.navigate(Route.Compta)
                        } else if (moduleKey == "admin_users") {
                            navController.navigate(Route.AdminUsers)
                        } else if (moduleKey == "gesdoc" || moduleKey == "documents") {
                            navController.navigate(Route.GesDoc)
                        }
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Route.Login) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable<Route.Hr> {
                val authVm: AuthViewModel = koinViewModel()
                val userProfile by authVm.currentUser.collectAsStateWithLifecycle()
                val roles = userProfile?.roles ?: emptyList()
                if (hasHrManagerAccess(roles)) {
                    HrDashboardScreen(
                        onNavigateToEmployees = { navController.navigate("hr_employees") },
                        onBack = { navController.popBackStack() },
                        onNavigateToEmployee = { id -> navController.navigate(Route.HrEmployee(id)) },
                        onNavigateToCreateEmployee = { navController.navigate("hr_employee_create") },
                        onNavigateToMyLeaves = { navController.navigate("my-leaves") },
                        onNavigateToTeamApproval = { navController.navigate("team-approval") },
                        onNavigateToPayslips = { navController.navigate("payslips") },
                        onNavigateToOrgChart = { navController.navigate("org-chart") },
                        onNavigateToMyPresences = { navController.navigate("my-presences") },
                        onNavigateToPresencesAdmin = { navController.navigate("presences-admin") },
                        onNavigateToPayrollAdmin = { navController.navigate("payroll-admin") },
                        onNavigateToMesDocuments = { navController.navigate("mes-documents") },
                        onNavigateToDemandesDocumentRH = { navController.navigate("demandes-document-rh") },
                        onNavigateToSoldesCongesAdmin = { navController.navigate("soldes-conges-admin") },
                        onNavigateToStatsRH = { navController.navigate("stats-rh") },
                    )
                } else {
                    HrAccessDeniedScreen(onBack = { navController.popBackStack() })
                }
            }
            composable("leave-request") {
                LeaveRequestScreen(
                    onBack = { navController.popBackStack() },
                    onSubmitted = { navController.popBackStack() },
                )
            }
            composable<Route.HrLeaveRequest> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.HrLeaveRequest>()
                LeaveRequestScreen(
                    employeeName = route.employeeName,
                    onBack = { navController.popBackStack() },
                    onSubmitted = { navController.popBackStack() },
                )
            }
            composable("my-leaves") {
                MyLeavesScreen(
                    onBack = { navController.popBackStack() },
                    onRequestLeave = { navController.navigate("leave-request") },
                )
            }
            composable("team-approval") {
                TeamApprovalScreen(onBack = { navController.popBackStack() })
            }
            composable("payslips") {
                val authViewModel: AuthViewModel = koinViewModel()
                val userProfile by authViewModel.currentUser.collectAsStateWithLifecycle()
                val profile = userProfile ?: DemoUserProfile
                if (hasPayslipAccess(profile.roles)) {
                    PayslipListScreen(
                        onBack = { navController.popBackStack() },
                        onPayslipClick = { id -> navController.navigate(Route.Payslip(id)) },
                    )
                } else {
                    Scaffold { padding ->
                        Box(
                            modifier = Modifier.padding(padding).fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Accès non autorisé")
                        }
                    }
                }
            }
            composable<Route.Payslip> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Payslip>()
                PayslipDetailScreen(
                    payslipId = route.id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("org-chart") {
                OrgChartScreen(onBack = { navController.popBackStack() })
            }
            composable("my-presences") {
                MyPresencesScreen(onBack = { navController.popBackStack() })
            }
            composable("presences-admin") {
                PresencesAdminScreen(onBack = { navController.popBackStack() })
            }
            composable("payroll-admin") {
                PayrollAdminScreen(onBack = { navController.popBackStack() })
            }
            composable("mes-documents") {
                MesDemandesDocumentScreen(onBack = { navController.popBackStack() })
            }
            composable("demandes-document-rh") {
                DemandesDocumentRHScreen(onBack = { navController.popBackStack() })
            }
            composable("soldes-conges-admin") {
                SoldesCongesAdminScreen(onBack = { navController.popBackStack() })
            }
            composable("stats-rh") {
                StatsRHScreen(onBack = { navController.popBackStack() })
            }
            composable("hr_employees") {
                EmployeeListScreen(
                    onEmployeeClick = { id -> navController.navigate(Route.HrEmployee(id)) },
                    onCreateEmployee = { navController.navigate("hr_employee_create") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.HrEmployee> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.HrEmployee>()
                EmployeeDetailScreen(
                    employeeId = route.id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Route.HrEmployeeEdit(it)) },
                    onNavigateToCreateContract = { empId -> navController.navigate(Route.HrContractCreate(empId)) },
                    onNewLeaveRequest = { _, employeeName ->
                        navController.navigate(Route.HrLeaveRequest(employeeName))
                    },
                    onPayslipClick = { id, name, number, position ->
                        navController.navigate(Route.HrPayslipDetail(id, name, number, position))
                    },
                )
            }
            composable<Route.HrPayslipDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.HrPayslipDetail>()
                PayslipDetailScreen(
                    payslipId = route.id,
                    onBack = { navController.popBackStack() },
                    employeeName = route.employeeName,
                    employeeNumber = route.employeeNumber,
                    employeePosition = route.employeePosition,
                )
            }
            composable("hr_employee_create") {
                EmployeeCreateScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }
            composable<Route.HrEmployeeEdit> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.HrEmployeeEdit>()
                EmployeeEditScreen(
                    employeeId = route.id,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable<Route.HrContractCreate> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.HrContractCreate>()
                ContractCreateScreen(
                    employeeId = route.employeeId,
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }
            composable<Route.Compta> {
                ComptaDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToInvoices = { navController.navigate("compta_invoices") },
                    onNavigateToPurchaseInvoices = { navController.navigate("compta_purchase_invoices") },
                    onNavigateToPayments = { navController.navigate("compta_payments") },
                    onNavigateToSupplierPayments = { navController.navigate("compta_supplier_payments") },
                )
            }
            composable("compta_invoices") {
                InvoiceListScreen(
                    onBack = { navController.popBackStack() },
                    onInvoiceClick = { id -> navController.navigate(Route.Invoice(id)) },
                    onCreateInvoice = { navController.navigate("compta_invoice_create") },
                )
            }
            composable("compta_invoice_create") {
                InvoiceCreateScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }
            composable<Route.Invoice> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Invoice>()
                InvoiceDetailScreen(
                    invoiceId = route.id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("compta_purchase_invoices") {
                PurchaseInvoiceListScreen(
                    onBack = { navController.popBackStack() },
                    onPurchaseInvoiceClick = { id -> navController.navigate(Route.PurchaseInvoice(id)) },
                )
            }
            composable<Route.PurchaseInvoice> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.PurchaseInvoice>()
                PurchaseInvoiceDetailScreen(
                    purchaseInvoiceId = route.id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("compta_payments") {
                PaymentListScreen(onBack = { navController.popBackStack() })
            }
            composable("compta_supplier_payments") {
                SupplierPaymentListScreen(onBack = { navController.popBackStack() })
            }
            composable<Route.AdminUsers> {
                val usersViewModel: UsersViewModel = koinViewModel()
                var showInviteDialog by remember { mutableStateOf(false) }
                UsersScreen(
                    onBack = { navController.popBackStack() },
                    onInviteUser = { showInviteDialog = true },
                    viewModel = usersViewModel,
                )
                if (showInviteDialog) {
                    InviteUserDialog(
                        onDismiss = { showInviteDialog = false },
                        onInvited = {
                            showInviteDialog = false
                            usersViewModel.loadUsers()
                        },
                    )
                }
            }
            composable<Route.GesDoc> {
                GesDocDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToDocument = { id -> navController.navigate(Route.GesDocDetail(id)) },
                    onNavigateToUpload = { navController.navigate(Route.GesDocUpload) },
                    onNavigateToAudit = { navController.navigate(Route.GesDocAudit) },
                )
            }
            composable<Route.GesDocUpload> {
                val uploadViewModel: GesDocUploadViewModel = koinViewModel()
                val fileLauncher = rememberFilePickerLauncher { pickedFile ->
                    uploadViewModel.setPickedFile(pickedFile.bytes, pickedFile.name, pickedFile.mimeType)
                }
                GesDocUploadScreen(
                    onBack = { navController.popBackStack() },
                    onPickFile = fileLauncher,
                    onNavigateToAudit = { navController.navigate(Route.GesDocAudit) },
                    onSubmitSuccess = { id -> navController.navigate(Route.GesDocExtraction(id)) },
                    viewModel = uploadViewModel,
                )
            }
            composable<Route.GesDocAudit> {
                GesDocAuditScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToUpload = { navController.navigate(Route.GesDocUpload) },
                )
            }
            composable<Route.GesDocExtraction> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.GesDocExtraction>()
                GesDocExtractionScreen(
                    documentId = route.id,
                    onBack = { navController.popBackStack() },
                    onNavigateToAudit = { navController.navigate(Route.GesDocAudit) },
                    onExtractionComplete = { id -> navController.navigate(Route.GesDocValidation(id)) },
                )
            }
            composable<Route.GesDocValidation> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.GesDocValidation>()
                GesDocValidationScreen(
                    documentId = route.id,
                    onBack = { navController.popBackStack() },
                    onNavigateToAudit = { navController.navigate(Route.GesDocAudit) },
                    onValidated = { id -> navController.navigate(Route.GesDocCertification(id)) },
                )
            }
            composable<Route.GesDocCertification> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.GesDocCertification>()
                GesDocCertificationScreen(
                    documentId = route.id,
                    onBack = { navController.popBackStack() },
                    onNavigateToAudit = { navController.navigate(Route.GesDocAudit) },
                    onNavigateToCompta = { navController.navigate(Route.Compta) },
                )
            }
            composable<Route.GesDocDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.GesDocDetail>()
                DocumentDetailScreen(
                    documentId = route.id,
                    onBack = { navController.popBackStack() },
                    onNavigateToOriginal = { id -> navController.navigate(Route.GesDocOriginal(id)) },
                )
            }
            composable<Route.GesDocOriginal> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.GesDocOriginal>()
                DocumentOriginalScreen(
                    documentId = route.id,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HrAccessDeniedScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Ressources Humaines", fontWeight = FontWeight.Bold, color = FormulooTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = FormulooTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooBackground),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = FormulooLabelGray,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "Accès non autorisé",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = FormulooTextPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Vous n'avez pas les droits nécessaires pour accéder au module Ressources Humaines. Contactez votre administrateur.",
                    fontSize = 14.sp,
                    color = FormulooLabelGray,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
