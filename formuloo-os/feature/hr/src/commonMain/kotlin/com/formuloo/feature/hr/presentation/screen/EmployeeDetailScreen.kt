package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooBlue
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooPurple
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.domain.model.Contract
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.EmployeeStatus
import com.formuloo.feature.hr.domain.model.LeaveBalance
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.domain.model.LeaveStatus
import com.formuloo.feature.hr.domain.model.Payslip
import com.formuloo.feature.hr.domain.model.PayslipStatus
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit = {},
    onNavigateToCreateContract: (String) -> Unit = {},
    onNewLeaveRequest: (employeeId: String, employeeName: String) -> Unit = { _, _ -> },
    onPayslipClick: (payslipId: String, employeeName: String, employeeNumber: String, employeePosition: String) -> Unit = { _, _, _, _ -> },
    viewModel: EmployeeDetailViewModel = koinViewModel(parameters = { parametersOf(employeeId) }),
) {
    val employeeState by viewModel.employeeState.collectAsStateWithLifecycle()
    val contractsState by viewModel.contractsState.collectAsStateWithLifecycle()
    val balancesState by viewModel.balancesState.collectAsStateWithLifecycle()
    val leavesState by viewModel.leavesState.collectAsStateWithLifecycle()
    val payslipsState by viewModel.payslipsState.collectAsStateWithLifecycle()
    val archiveError by viewModel.archiveError.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    val tabs: List<Pair<String, ImageVector>> = listOf(
        "Infos" to Icons.Filled.Person,
        "Contrat" to Icons.Filled.Description,
        "Congés" to Icons.Filled.DateRange,
        "Paie" to Icons.Filled.Receipt,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooBackground),
    ) {
        when (val state = employeeState) {
            is UiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FormulooPrimary)
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
            }

            is UiState.Error -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FormulooPrimary)
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = FormulooError)
                }
            }

            is UiState.Success -> {
                val employee = state.data
                val activeContract = (contractsState as? UiState.Success)?.data?.firstOrNull { it.isActive }

                // ── Hero header (teal block) ───────────────────────────
                Column(
                    modifier = Modifier
                        .background(FormulooPrimary)
                        .fillMaxWidth(),
                ) {
                    // Navigation row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = Color.White,
                            )
                        }
                        Row {
                            IconButton(onClick = { onEdit(employee.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Modifier", tint = Color.White)
                            }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Plus d'options", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Archiver l'employé", color = FormulooError) },
                                        onClick = {
                                            showMenu = false
                                            showArchiveConfirm = true
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (showArchiveConfirm) {
                        AlertDialog(
                            onDismissRequest = { showArchiveConfirm = false },
                            title = { Text("Archiver l'employé") },
                            text = { Text("Cette action désactivera le compte de ${employee.firstName} ${employee.lastName}. Vous pourrez le réactiver ultérieurement.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showArchiveConfirm = false
                                    viewModel.archiveEmployee(onSuccess = onBack)
                                }) {
                                    Text("Archiver", color = FormulooError)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showArchiveConfirm = false }) {
                                    Text("Annuler")
                                }
                            },
                        )
                    }

                    if (archiveError != null) {
                        AlertDialog(
                            onDismissRequest = { viewModel.clearArchiveError() },
                            title = { Text("Erreur") },
                            text = { Text(archiveError ?: "") },
                            confirmButton = {
                                TextButton(onClick = { viewModel.clearArchiveError() }) {
                                    Text("OK")
                                }
                            },
                        )
                    }

                    // Avatar + name / title / chips row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = employee.initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = employee.fullName,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = employee.position ?: employee.department ?: "",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HeroStatusChip(employee.status)
                                deptCode(employee.department)?.let { HeroChip(it) }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.5.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Appeler", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = { },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = FormulooPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("E-mail", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }

                // ── Metrics band (white) ───────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FormulooSurface)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricColumn(
                        label = "MATRICULE",
                        value = employee.employeeNumber,
                        modifier = Modifier.weight(1f),
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp), color = FormulooOutline)
                    MetricColumn(
                        label = "CONTRAT",
                        value = activeContract?.type?.name ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp), color = FormulooOutline)
                    MetricColumn(
                        label = "ANCIENNETÉ",
                        value = ancienneteSince(employee.hireDate),
                        modifier = Modifier.weight(1f),
                    )
                    VerticalDivider(modifier = Modifier.height(36.dp), color = FormulooOutline)
                    MetricColumn(
                        label = "SALAIRE BR",
                        value = activeContract?.let { formatSalary(it.grossSalary) } ?: "—",
                        modifier = Modifier.weight(1f),
                    )
                }

                // ── Tab bar ────────────────────────────────────────────
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = FormulooSurface,
                    contentColor = FormulooPrimary,
                ) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            selectedContentColor = FormulooPrimary,
                            unselectedContentColor = FormulooOnSurfaceVariant,
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            },
                        )
                    }
                }

                // ── Tab content ────────────────────────────────────────
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> TabInfos(employee)
                        1 -> TabContrats(
                            state = contractsState,
                            onCreateContract = { onNavigateToCreateContract(employee.id) },
                        )
                        2 -> TabConges(
                            balancesState = balancesState,
                            leavesState = leavesState,
                            onNewRequest = { onNewLeaveRequest(employee.id, employee.fullName) },
                        )
                        3 -> TabPaie(
                            state = payslipsState,
                            onPayslipClick = { payslipId ->
                                onPayslipClick(
                                    payslipId,
                                    employee.fullName,
                                    employee.employeeNumber,
                                    employee.position ?: "",
                                )
                            },
                        )
                    }
                }
            }

            else -> {}
        }
    }
}

// ── Tab: Infos ─────────────────────────────────────────────────────────────

@Composable
private fun TabInfos(employee: Employee) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            InfoCard(
                title = "Coordonnées",
                icon = Icons.Filled.Email,
                rows = listOf(
                    "E-mail" to employee.email,
                    "Téléphone" to employee.phone,
                    "Localisation" to (employee.address?.substringAfterLast(",")?.trim() ?: "—"),
                    "Adresse" to (employee.address ?: "—"),
                ),
            )
        }
        item {
            InfoCard(
                title = "Informations personnelles",
                icon = Icons.Filled.Person,
                rows = listOf(
                    "Genre" to when (employee.gender.name) {
                        "M" -> "Masculin"
                        "F" -> "Féminin"
                        else -> "Autre"
                    },
                    "Nationalité" to (employee.nationality ?: "—"),
                    "N° national" to if (employee.numeroCnps != null) "••••••••" else "—",
                    "Date d'embauche" to formatDate(employee.hireDate),
                ),
            )
        }
        item {
            InfoCard(
                title = "Hiérarchie",
                icon = Icons.Filled.AccountTree,
                rows = listOf(
                    "Département" to (employee.department ?: "—"),
                    "Poste" to (employee.position ?: "—"),
                    "Responsable" to (employee.managerName ?: "—"),
                    "Type" to employee.employeeType.name.lowercase().replaceFirstChar { it.uppercase() },
                ),
            )
        }
    }
}

// ── Tab: Contrats ──────────────────────────────────────────────────────────

@Composable
private fun TabContrats(state: UiState<List<Contract>>, onCreateContract: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onCreateContract,
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nouveau contrat", fontSize = 13.sp)
            }
        }
        when (state) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FormulooPrimary)
            }

            is UiState.Empty, is UiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Aucun contrat disponible.", color = FormulooOnSurfaceVariant)
            }

            is UiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.data) { contract ->
                    ContractCard(contract)
                }
            }
        }
    }
}

@Composable
private fun ContractCard(contract: Contract) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = FormulooPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Contrat de travail",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = FormulooTextPrimary,
                    )
                }
                ActiveBadge(isActive = contract.isActive)
            }
            Spacer(Modifier.height(12.dp))
            val rows = buildList {
                add("Type" to contract.type.name)
                add("Début" to formatDate(contract.startDate))
                add("Échéance" to (contract.endDate?.let { formatDate(it) } ?: "Indéterminée"))
                add("Salaire brut" to formatSalaryCurrency(contract.grossSalary, contract.currency))
                add("Statut" to if (contract.isActive) "Actif" else "Expiré")
            }
            rows.forEach { (label, value) ->
                InfoRow(label = label, value = value)
            }
        }
    }
}

// ── Tab: Congés ────────────────────────────────────────────────────────────

@Composable
private fun TabConges(
    balancesState: UiState<List<LeaveBalance>>,
    leavesState: UiState<List<LeaveRequest>>,
    onNewRequest: () -> Unit,
) {
    val leaveCount = (leavesState as? UiState.Success)?.data?.size ?: 0
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { LeaveBalanceCard(balancesState) }

        item {
            Text(
                "DEMANDES ($leaveCount)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FormulooLabelGray,
                letterSpacing = 0.8.sp,
            )
        }

        when (leavesState) {
            is UiState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = FormulooPrimary)
                }
            }
            is UiState.Empty, is UiState.Error -> item {
                Text("Aucune demande de congé en cours.", color = FormulooOnSurfaceVariant, fontSize = 14.sp)
            }
            is UiState.Success -> items(leavesState.data) { leave ->
                LeaveRequestItem(leave)
            }
        }

        item {
            OutlinedButton(
                onClick = onNewRequest,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, FormulooPrimary),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = FormulooMint,
                    contentColor = FormulooPrimary,
                ),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nouvelle demande de congé", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun LeaveBalanceCard(state: UiState<List<LeaveBalance>>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FormulooMint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.DateRange, null, tint = FormulooPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Solde de congés", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
            }
            Spacer(Modifier.height(12.dp))
            when (state) {
                is UiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = FormulooPrimary,
                )
                is UiState.Empty, is UiState.Error -> Text(
                    "Aucun solde disponible.",
                    color = FormulooOnSurfaceVariant,
                    fontSize = 14.sp,
                )
                is UiState.Success -> state.data.forEachIndexed { i, balance ->
                    if (i > 0) HorizontalDivider(thickness = 0.5.dp, color = FormulooOutline.copy(alpha = 0.6f))
                    LeaveBalanceRow(balance)
                }
            }
        }
    }
}

@Composable
private fun LeaveBalanceRow(balance: LeaveBalance) {
    val label = leaveTypeLabel(balance.typeConge)
    val progress = if (balance.joursAcquis > 0) {
        (balance.joursPris / balance.joursAcquis).toFloat().coerceIn(0f, 1f)
    } else 0f
    val barColor = leaveTypeColor(balance.typeConge)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 13.sp, color = FormulooTextPrimary, fontWeight = FontWeight.Medium)
            Text(
                "${balance.joursPris.toInt()} / ${balance.joursAcquis.toInt()} j",
                fontSize = 12.sp,
                color = FormulooLabelGray,
            )
        }
        Spacer(Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun LeaveRequestItem(leave: LeaveRequest) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FormulooMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DateRange, null, tint = FormulooPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(leave.leaveTypeLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FormulooTextPrimary)
                Text(
                    "${formatDate(leave.startDate)} → ${formatDate(leave.endDate)}",
                    fontSize = 12.sp,
                    color = FormulooLabelGray,
                )
                Text(
                    "${leave.days} jour${if (leave.days > 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }
            LeaveStatusBadge(leave.status)
        }
    }
}

@Composable
private fun LeaveStatusBadge(status: LeaveStatus) {
    val (text, bg, fg) = when (status) {
        LeaveStatus.PENDING -> Triple("En attente", FormulooSecondaryBg, FormulooSecondary)
        LeaveStatus.APPROVED -> Triple("Approuvé", FormulooMint, FormulooPrimary)
        LeaveStatus.REJECTED -> Triple("Refusé", FormulooErrorBg, FormulooError)
        LeaveStatus.ANNULE -> Triple("Annulé", Color(0xFFF5F5F5), FormulooLabelGray)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

private fun leaveTypeColor(typeConge: String): Color = when (typeConge) {
    "annuel" -> FormulooPrimary
    "maladie" -> FormulooError
    "maternite", "paternite" -> FormulooPurple
    "formation" -> FormulooBlue
    else -> FormulooLabelGray
}

private fun leaveTypeLabel(typeConge: String): String = when (typeConge) {
    "annuel" -> "Congé annuel"
    "maladie" -> "Congé maladie"
    "maternite" -> "Congé maternité"
    "paternite" -> "Congé paternité"
    "sans_solde" -> "Sans solde"
    "exceptionnel" -> "Exceptionnel"
    "recuperation" -> "Récupération"
    "formation" -> "Formation"
    "deces" -> "Congé décès"
    else -> typeConge
}

// ── Tab: Paie ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabPaie(
    state: UiState<List<Payslip>>,
    onPayslipClick: (String) -> Unit,
) {
    val count = (state as? UiState.Success)?.data?.size ?: 0
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "BULLETINS DE PAIE ($count)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FormulooLabelGray,
                letterSpacing = 0.8.sp,
            )
        }
        when (state) {
            is UiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = FormulooPrimary)
                }
            }
            is UiState.Empty, is UiState.Error -> item {
                Text("Aucun bulletin de paie disponible.", color = FormulooOnSurfaceVariant, fontSize = 14.sp)
            }
            is UiState.Success -> items(state.data) { payslip ->
                PayslipSummaryCard(payslip = payslip, onClick = { onPayslipClick(payslip.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayslipSummaryCard(payslip: Payslip, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FormulooMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Receipt, null, tint = FormulooPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(payslip.period, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
                    Text(
                        "${formatPayAmount(payslip.netSalary)} ${payslip.currency}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FormulooTextPrimary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Brut ${formatPayCompact(payslip.gross)} · Net ${formatPayCompact(payslip.netSalary)}",
                        fontSize = 12.sp,
                        color = FormulooLabelGray,
                    )
                    PayslipStatusPill(payslip.status)
                }
            }
        }
    }
}

@Composable
private fun PayslipStatusPill(status: PayslipStatus) {
    val (bg, fg, label) = when (status) {
        PayslipStatus.BROUILLON -> Triple(FormulooOutline, FormulooOnSurfaceVariant, "Brouillon")
        PayslipStatus.VALIDE -> Triple(FormulooSecondaryBg, FormulooSecondary, "Validé")
        PayslipStatus.PAYE -> Triple(FormulooMint, FormulooPrimary, "• Payé")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 11.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatPayAmount(value: Double): String {
    val n = value.toLong()
    val s = n.toString()
    val sb = StringBuilder()
    for ((i, c) in s.reversed().withIndex()) {
        if (i != 0 && i % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return sb.reverse().toString()
}

private fun formatPayCompact(value: Double): String {
    return when {
        value >= 1_000_000 -> {
            val m = value / 1_000_000
            val s = "%.2f".format(m).trimEnd('0').trimEnd('.')
            "$s M"
        }
        value >= 1_000 -> "${(value / 1_000).toLong()} k"
        else -> "${value.toLong()}"
    }
}

// ── Shared composables ─────────────────────────────────────────────────────

@Composable
private fun InfoCard(title: String, icon: ImageVector, rows: List<Pair<String, String>>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FormulooMint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = FormulooPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
            }
            Spacer(Modifier.height(12.dp))
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    HorizontalDivider(thickness = 0.5.dp, color = FormulooOutline.copy(alpha = 0.6f))
                }
                InfoRow(label = label, value = value)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = FormulooLabelGray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FormulooTextPrimary)
    }
}

@Composable
private fun MetricColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = FormulooLabelGray,
            letterSpacing = 0.4.sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = FormulooTextPrimary,
        )
    }
}

@Composable
private fun HeroStatusChip(status: EmployeeStatus) {
    val (label, dotColor) = when (status) {
        EmployeeStatus.ACTIVE -> "Actif" to Color(0xFF4CAF50)
        EmployeeStatus.ON_LEAVE -> "En congé" to FormulooSecondary
        EmployeeStatus.INACTIVE -> "Inactif" to Color.White.copy(alpha = 0.6f)
        EmployeeStatus.TERMINATED -> "Licencié" to Color(0xFFEF5350)
        EmployeeStatus.SUSPENDED -> "Suspendu" to Color(0xFFEF5350)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(dotColor),
        )
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun HeroChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun ActiveBadge(isActive: Boolean) {
    val bg = if (isActive) FormulooMint else Color(0xFFF5F5F5)
    val fg = if (isActive) FormulooPrimary else FormulooOnSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = if (isActive) "Actif" else "Expiré",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
        )
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun deptCode(name: String?): String? {
    if (name == null) return null
    val n = name.lowercase()
        .replace("é", "e").replace("è", "e").replace("ê", "e")
        .replace("à", "a").replace("â", "a")
        .replace("ô", "o").replace("ù", "u").replace("î", "i")
    return when {
        n.contains("direction") && n.contains("gen") -> "DG"
        n.contains("technique") || n.contains("tech") || n.contains("informatique") -> "TECH"
        n.contains("commercial") || n.contains("vente") -> "COM"
        n.contains("finance") || n.contains("comptab") -> "FIN"
        n.contains("operation") -> "OPS"
        n.contains("marketing") -> "MKT"
        n.contains("ressource") || n.contains("human") -> "RH"
        else -> name.take(3).uppercase()
    }
}

private fun ancienneteSince(hireDate: String): String {
    return try {
        val year = hireDate.substring(0, 4).toInt()
        val month = hireDate.substring(5, 7).toInt()
        val currentYear = 2026
        val currentMonth = 7
        val diffMonths = (currentYear - year) * 12 + (currentMonth - month)
        val years = diffMonths / 12
        val months = diffMonths % 12
        when {
            years <= 0 && months <= 0 -> "< 1 mois"
            years <= 0 -> "$months mois"
            else -> "$years ans"
        }
    } catch (_: Exception) {
        "—"
    }
}

private fun formatDate(iso: String): String {
    return try {
        val parts = iso.split("-")
        val months = listOf(
            "", "janv.", "févr.", "mars", "avr.", "mai", "juin",
            "juil.", "août", "sept.", "oct.", "nov.", "déc.",
        )
        "${parts[2].toInt()} ${months[parts[1].toInt()]} ${parts[0]}"
    } catch (_: Exception) {
        iso
    }
}

private fun formatSalary(amount: Double): String {
    return when {
        amount >= 1_000_000 -> {
            val whole = (amount / 1_000_000).toLong()
            val dec = (((amount / 1_000_000) - whole) * 10).toLong()
            if (dec == 0L) "${whole} M" else "${whole},${dec} M"
        }
        amount >= 1_000 -> "${(amount / 1_000).toLong()} k"
        else -> "${amount.toLong()}"
    }
}

private fun formatSalaryCurrency(amount: Double, currency: String): String {
    val formatted = "%,.0f".format(amount).replace(",", " ")
    return "$formatted $currency/mois"
}
