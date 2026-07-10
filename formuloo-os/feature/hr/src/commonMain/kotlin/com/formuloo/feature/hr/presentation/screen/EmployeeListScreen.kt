package com.formuloo.feature.hr.presentation.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.InitialsAvatar
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.EmployeeStatus
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeListViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    onEmployeeClick: (String) -> Unit,
    onCreateEmployee: () -> Unit,
    onBack: () -> Unit,
    viewModel: EmployeeListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val isRefreshing = uiState is UiState.Loading

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Employés", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FormulooPrimary,
                    titleContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEmployee,
                containerColor = FormulooPrimary,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un employé")
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Barre de recherche ──────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.search(it) },
                    placeholder = { Text("Rechercher un employé, matricule, poste…") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = FormulooOnSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FormulooPrimary,
                        unfocusedBorderColor = FormulooOutline,
                        focusedContainerColor = FormulooSurface,
                        unfocusedContainerColor = FormulooSurface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )

                // ── Chips de filtre ─────────────────────────────────────
                val filters = listOf(
                    null to "Tous",
                    "active" to "Actifs",
                    "on_leave" to "En congé",
                    "stagiaire" to "Stagiaires",
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filters) { (value, label) ->
                        StatusChip(
                            label = label,
                            selected = selectedStatus == value,
                            onClick = { viewModel.filterByStatus(value) },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Liste / états ───────────────────────────────────────
                when (val state = uiState) {
                    is UiState.Loading -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(5) { EmployeeCardSkeleton() }
                        }
                    }
                    is UiState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.data, key = { it.id }) { employee ->
                                EmployeeCard(
                                    employee = employee,
                                    onClick = { onEmployeeClick(employee.id) },
                                )
                            }
                        }
                    }
                    is UiState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔍", fontSize = 48.sp)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Aucun employé trouvé",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FormulooOnSurfaceVariant,
                                )
                                if (!searchQuery.isBlank()) {
                                    Text(
                                        "Essayez un autre terme de recherche",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = FormulooOnSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = state.message,
                                color = FormulooError,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeCard(employee: Employee, onClick: () -> Unit) {
    val avatarColor = avatarColorFor(employee.employeeNumber)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(
                initials = employee.initials,
                size = 48.dp,
                backgroundColor = avatarColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.fullName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = FormulooTextPrimary,
                )
                Text(
                    text = employee.position ?: employee.department ?: employee.employeeNumber,
                    fontSize = 13.sp,
                    color = FormulooOnSurfaceVariant,
                )
                if (employee.department != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        employee.department?.let { dept ->
                            MiniTag(label = dept, background = FormulooMint, textColor = FormulooPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusBadge(status = employee.status)
        }
    }
}

@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) FormulooPrimary else FormulooSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else FormulooTextPrimary,
        )
    }
}

@Composable
private fun StatusBadge(status: EmployeeStatus) {
    val (bg, fg, label) = when (status) {
        EmployeeStatus.ACTIVE -> Triple(FormulooMint, FormulooPrimary, "Actif")
        EmployeeStatus.ON_LEAVE -> Triple(Color(0xFFFFF3E0), FormulooSecondary, "En congé")
        EmployeeStatus.INACTIVE -> Triple(Color(0xFFF5F5F5), FormulooOnSurfaceVariant, "Inactif")
        EmployeeStatus.TERMINATED -> Triple(Color(0xFFFFEBEE), FormulooError, "Licencié")
        EmployeeStatus.SUSPENDED -> Triple(Color(0xFFFFEBEE), FormulooError, "Suspendu")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

@Composable
private fun MiniTag(label: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = label, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmployeeCardSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer-alpha",
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(FormulooOutline.copy(alpha = alpha)))
            Spacer(Modifier.width(12.dp))
            Column {
                Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.5f).clip(RoundedCornerShape(4.dp)).background(FormulooOutline.copy(alpha = alpha)))
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.height(12.dp).fillMaxWidth(0.35f).clip(RoundedCornerShape(4.dp)).background(FormulooOutline.copy(alpha = alpha)))
            }
        }
    }
}

private fun avatarColorFor(seed: String): Color {
    val colors = listOf(
        Color(0xFF1A6B5A), Color(0xFF7C3AED), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981),
    )
    return colors[seed.hashCode().and(0x7FFFFFFF) % colors.size]
}
