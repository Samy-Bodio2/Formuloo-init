package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.formuloo.core.common.model.PendingRequest
import com.formuloo.core.designsystem.BadgeTone
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooBlueBg
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooPurple
import com.formuloo.core.designsystem.FormulooPurpleBg
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextField
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.InitialsAvatar
import com.formuloo.core.designsystem.PendingRequestItem
import com.formuloo.core.designsystem.StatusBadge
import com.formuloo.feature.hr.domain.model.Contract
import com.formuloo.feature.hr.domain.model.ContractType
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.EmployeeStatus
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeListViewModel
import com.formuloo.feature.hr.presentation.viewmodel.HrDashboardViewModel
import com.formuloo.feature.hr.presentation.viewmodel.HrStats
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrDashboardScreen(
    onNavigateToEmployees: () -> Unit,
    onBack: () -> Unit,
    onNavigateToEmployee: (String) -> Unit = {},
    onNavigateToCreateEmployee: () -> Unit = {},
    onNavigateToMyLeaves: () -> Unit = {},
    onNavigateToTeamApproval: () -> Unit = {},
    onNavigateToPayslips: () -> Unit = {},
    onNavigateToOrgChart: () -> Unit = {},
    onNavigateToMyPresences: () -> Unit = {},
    onNavigateToPresencesAdmin: () -> Unit = {},
    onNavigateToPayrollAdmin: () -> Unit = {},
    onNavigateToMesDocuments: () -> Unit = {},
    onNavigateToDemandesDocumentRH: () -> Unit = {},
    onNavigateToSoldesCongesAdmin: () -> Unit = {},
    onNavigateToStatsRH: () -> Unit = {},
    viewModel: HrDashboardViewModel = koinViewModel(),
    employeeListViewModel: EmployeeListViewModel = koinViewModel(),
) {
    val pendingLeavesState by viewModel.pendingLeaves.collectAsStateWithLifecycle()
    val hrStats by viewModel.hrStats.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    // État pour la confirmation de rejet
    var rejectTargetId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    // Boîte de dialogue de rejet
    rejectTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { rejectTargetId = null; rejectReason = "" },
            title = { Text("Motif de refus") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    placeholder = { Text("Indiquez le motif du refus…") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FormulooPrimary,
                        unfocusedBorderColor = FormulooOutline,
                    ),
                    minLines = 2,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rejectReason.isNotBlank()) {
                            viewModel.rejectLeave(id, rejectReason)
                            rejectTargetId = null
                            rejectReason = ""
                        }
                    },
                ) { Text("Refuser", color = FormulooError) }
            },
            dismissButton = {
                TextButton(onClick = { rejectTargetId = null; rejectReason = "" }) {
                    Text("Annuler")
                }
            },
        )
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ressources Humaines", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = FormulooTextPrimary)
                        Text(
                            "Module RH · ${hrStats.totalEmployees} collaborateurs",
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = FormulooTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Tune, contentDescription = "Filtres", tint = FormulooTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooBackground),
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNavigateToCreateEmployee,
                    containerColor = FormulooPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter un employé")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // ── Raccourcis discrets (Mes congés / Approbations / Paie / Organigramme) ──
            // Choix retenu : la référence ne montre pas ces raccourcis (focus liste
            // employés). Plutôt que de les supprimer silencieusement, ils sont conservés
            // sous forme d'icônes secondaires compactes et sans libellé pour ne pas
            // alourdir l'écran par rapport à la référence.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAccessIconButton(Icons.Filled.BeachAccess, "Mes congés", onNavigateToMyLeaves)
                QuickAccessIconButton(Icons.Filled.FactCheck, "Approbations", onNavigateToTeamApproval)
                QuickAccessIconButton(Icons.Filled.Payments, "Paie", onNavigateToPayslips)
                QuickAccessIconButton(Icons.Filled.AccountTree, "Organigramme", onNavigateToOrgChart)
                QuickAccessIconButton(Icons.Filled.Today, "Mes présences", onNavigateToMyPresences)
                QuickAccessIconButton(Icons.Filled.HowToReg, "Pointage", onNavigateToPresencesAdmin)
                QuickAccessIconButton(Icons.Filled.Savings, "Génération paie", onNavigateToPayrollAdmin)
                QuickAccessIconButton(Icons.Filled.Article, "Mes documents", onNavigateToMesDocuments)
                QuickAccessIconButton(Icons.Filled.Inbox, "Demandes docs RH", onNavigateToDemandesDocumentRH)
                QuickAccessIconButton(Icons.Filled.DateRange, "Soldes congés", onNavigateToSoldesCongesAdmin)
                QuickAccessIconButton(Icons.Filled.Analytics, "Stats RH", onNavigateToStatsRH)
            }

            // ── Cartes statistiques (données réelles) ─────────────────
            HrStatCards(stats = hrStats)

            // ── Onglets (restylés sobrement : texte + soulignement, déjà sans pilules) ──
            val tabs = listOf("Employés", "Congés", "Contrats")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = FormulooSurface,
                contentColor = FormulooPrimary,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                0 -> EmployeesTabContent(viewModel = employeeListViewModel, onEmployeeClick = onNavigateToEmployee)
                1 -> LeavesTabContent(
                    state = pendingLeavesState,
                    onApprove = { id -> viewModel.approveLeave(id) },
                    onReject = { id -> rejectTargetId = id },
                )
                2 -> ContractsTabContent()
            }
        }
    }
}

@Composable
private fun QuickAccessIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = FormulooOnSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HrStatCards(stats: HrStats) {
    val activePct = if (stats.totalEmployees > 0)
        (stats.activeEmployees * 100 / stats.totalEmployees)
    else 0
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HrStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.People,
            iconBg = FormulooMint,
            iconTint = FormulooPrimary,
            value = if (stats.totalEmployees > 0) "${stats.totalEmployees}" else "—",
            label = "Effectif total",
            subLabel = if (stats.pendingLeavesCount > 0) "${stats.pendingLeavesCount} en attente" else "à jour",
        )
        HrStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.CheckCircle,
            iconBg = FormulooMint,
            iconTint = FormulooPrimary,
            value = if (stats.activeEmployees > 0) "${stats.activeEmployees}" else "—",
            label = "Actifs",
            subLabel = if (activePct > 0) "$activePct % effectif" else "chargement",
        )
        HrStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Event,
            iconBg = FormulooSecondaryBg,
            iconTint = FormulooSecondary,
            value = "${stats.onLeaveEmployees}",
            label = "En congé",
            subLabel = "en cours",
        )
    }
}

@Composable
private fun HrStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    label: String,
    subLabel: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FormulooTextPrimary)
            Text(label, fontSize = 12.sp, color = FormulooTextPrimary, fontWeight = FontWeight.Medium)
            Text(subLabel, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
        }
    }
}

private val departmentChipCodes = listOf("FIN", "TECH", "COM", "OPS", "MKT", "RH")

private fun stripAccents(text: String): String = text
    .replace(Regex("[éèêë]"), "e")
    .replace(Regex("[àâ]"), "a")
    .replace(Regex("[ôö]"), "o")
    .replace(Regex("[ûü]"), "u")
    .replace(Regex("[îï]"), "i")
    .replace("ç", "c")

private fun departmentCode(name: String?): String {
    if (name.isNullOrBlank()) return "—"
    val known = mapOf(
        "ressources humaines" to "RH",
        "finance" to "FIN",
        "comptabilite" to "FIN",
        "technique" to "TECH",
        "informatique" to "TECH",
        "commercial" to "COM",
        "ventes" to "COM",
        "operations" to "OPS",
        "marketing" to "MKT",
        "direction generale" to "DG",
        "direction" to "DG",
    )
    val normalized = stripAccents(name.trim().lowercase())
    return known[normalized] ?: name.take(3).uppercase()
}

@Composable
private fun EmployeesTabContent(viewModel: EmployeeListViewModel, onEmployeeClick: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeContractByEmployee by viewModel.activeContractByEmployee.collectAsStateWithLifecycle()
    var selectedDept by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        FormulooTextField(
            value = searchQuery,
            onValueChange = viewModel::search,
            label = "",
            placeholder = "Rechercher un employé, matricule, poste",
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(departmentChipCodes) { code ->
                DepartmentFilterChip(
                    label = code,
                    selected = selectedDept == code,
                    onClick = { selectedDept = if (selectedDept == code) null else code },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when (val state = uiState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FormulooPrimary)
            }
            is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Aucun employé trouvé", color = FormulooOnSurfaceVariant)
            }
            is UiState.Success -> {
                val employees = state.data.filter { employee ->
                    selectedDept == null || departmentCode(employee.department) == selectedDept
                }
                if (employees.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun employé ne correspond à ce filtre.", color = FormulooOnSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(employees, key = { it.id }) { employee ->
                            HrEmployeeCard(
                                employee = employee,
                                contract = activeContractByEmployee[employee.id],
                                onClick = { onEmployeeClick(employee.id) },
                            )
                        }
                    }
                }
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(state.message, color = FormulooError)
            }
        }
    }
}

@Composable
private fun DepartmentFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) FormulooPrimary else FormulooSurface)
            .border(
                BorderStroke(1.dp, if (selected) FormulooPrimary else FormulooOutline),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else FormulooTextPrimary,
        )
    }
}

private fun employeeStatusLabel(status: EmployeeStatus): String = when (status) {
    EmployeeStatus.ACTIVE -> "Actif"
    EmployeeStatus.ON_LEAVE -> "En congé"
    EmployeeStatus.INACTIVE -> "Inactif"
    EmployeeStatus.TERMINATED -> "Sorti"
    EmployeeStatus.SUSPENDED -> "Suspendu"
}

private fun employeeStatusTone(status: EmployeeStatus): BadgeTone = when (status) {
    EmployeeStatus.ACTIVE -> BadgeTone.SUCCESS
    EmployeeStatus.ON_LEAVE -> BadgeTone.WARNING
    EmployeeStatus.INACTIVE -> BadgeTone.NEUTRAL
    EmployeeStatus.TERMINATED -> BadgeTone.DANGER
    EmployeeStatus.SUSPENDED -> BadgeTone.DANGER
}

private val employeeAvatarPalette = listOf(
    FormulooPrimary, FormulooPurple, FormulooSecondary, FormulooError, Color(0xFF3B82F6), Color(0xFF10B981),
)

private fun employeeAvatarColor(seed: String): Color =
    employeeAvatarPalette[kotlin.math.abs(seed.hashCode()) % employeeAvatarPalette.size]

private fun contractTypeLabel(type: ContractType): String = when (type) {
    ContractType.CDI -> "CDI"
    ContractType.CDD -> "CDD"
    ContractType.INTERIM -> "Intérim"
    ContractType.STAGE -> "Stage"
    ContractType.FREELANCE -> "Freelance"
}

private fun contractTypeColors(type: ContractType): Pair<Color, Color> = when (type) {
    ContractType.CDI -> FormulooMint to FormulooPrimary
    ContractType.CDD, ContractType.INTERIM -> FormulooSecondaryBg to FormulooSecondary
    ContractType.STAGE -> FormulooPurpleBg to FormulooPurple
    ContractType.FREELANCE -> FormulooBlueBg to Color(0xFF1976D2)
}

private fun formatSalaryAmount(amount: Double): String = when {
    amount >= 1_000_000 -> {
        val millions = kotlin.math.round(amount / 1_000_000 * 100) / 100
        val text = if (millions == millions.toLong().toDouble()) {
            millions.toLong().toString()
        } else {
            millions.toString().trimEnd('0').trimEnd('.')
        }
        "${text.replace(".", ",")} M"
    }
    amount >= 1_000 -> "${(amount / 1_000).toInt()} k"
    else -> amount.toInt().toString()
}

@Composable
private fun HrEmployeeCard(employee: Employee, contract: Contract?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            InitialsAvatar(
                initials = employee.initials,
                size = 44.dp,
                backgroundColor = employeeAvatarColor(employee.id),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
                Text(
                    employee.position ?: employee.employeeNumber,
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeptCodeTag(departmentCode(employee.department))
                    if (contract != null) {
                        val (bg, fg) = contractTypeColors(contract.type)
                        SmallTag(label = contractTypeLabel(contract.type), background = bg, textColor = fg)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(
                    label = employeeStatusLabel(employee.status),
                    tone = employeeStatusTone(employee.status),
                    dot = true,
                )
                Spacer(Modifier.height(8.dp))
                if (contract != null) {
                    Text(
                        formatSalaryAmount(contract.grossSalary),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FormulooTextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeptCodeTag(label: String) {
    SmallTag(label = label, background = FormulooBackground, textColor = FormulooOnSurfaceVariant)
}

@Composable
private fun SmallTag(label: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(text = label, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LeavesTabContent(
    state: UiState<List<LeaveRequest>>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    when (state) {
        is UiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = FormulooPrimary)
        }
        is UiState.Empty -> Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Aucune demande en attente",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = FormulooOnSurfaceVariant,
                )
            }
        }
        is UiState.Success -> {
            val leaves = state.data
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item {
                    Text(
                        "DEMANDES (${leaves.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FormulooOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(leaves, key = { it.id }) { leave ->
                    PendingRequestItem(
                        request = PendingRequest(
                            id = leave.id,
                            employeeName = leave.employeeName,
                            initials = leave.employeeInitials,
                            type = leave.leaveTypeLabel,
                            duration = "${leave.days}j",
                        ),
                        onApprove = { onApprove(leave.id) },
                        onReject = { onReject(leave.id) },
                    )
                }
            }
        }
        is UiState.Error -> Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(state.message, color = FormulooError)
        }
        else -> {}
    }
}

@Composable
private fun ContractsTabContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📋", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Gestion des contrats à venir.",
                color = FormulooOnSurfaceVariant,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
