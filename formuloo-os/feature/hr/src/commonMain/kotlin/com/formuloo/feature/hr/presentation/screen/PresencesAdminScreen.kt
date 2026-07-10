package com.formuloo.feature.hr.presentation.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooDropdownField
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooBlue
import com.formuloo.core.designsystem.FormulooDeepOrange
import com.formuloo.core.designsystem.FormulooGreen
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooPurple
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTeal
import com.formuloo.core.designsystem.FormulooTextField
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.InitialsAvatar
import com.formuloo.feature.hr.domain.model.Employee
import com.formuloo.feature.hr.domain.model.Presence
import com.formuloo.feature.hr.domain.model.PresenceStatus
import kotlin.math.abs
import com.formuloo.feature.hr.presentation.viewmodel.PresenceCreateForm
import com.formuloo.feature.hr.presentation.viewmodel.PresencesAdminViewModel
import org.koin.compose.viewmodel.koinViewModel

private val STATUT_FILTERS = listOf(
    null to "Tous",
    "present" to "Présent",
    "absent" to "Absent",
    "retard" to "Retard",
    "conge" to "Congé",
    "ferie" to "Férié",
)

private val STATUT_OPTIONS = listOf("present", "absent", "retard", "conge", "ferie")
private val STATUT_LABELS = mapOf(
    "present" to "Présent",
    "absent" to "Absent",
    "retard" to "Retard",
    "conge" to "Congé",
    "ferie" to "Férié",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresencesAdminScreen(
    onBack: () -> Unit,
    viewModel: PresencesAdminViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val statutFilter by viewModel.statutFilter.collectAsStateWithLifecycle()
    val dateDebut by viewModel.dateDebut.collectAsStateWithLifecycle()
    val dateFin by viewModel.dateFin.collectAsStateWithLifecycle()
    val createForm by viewModel.createForm.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    // Get today's date for the default new-presence form
    val now = remember { java.util.Calendar.getInstance() }
    val todayIso = remember {
        val y = now.get(java.util.Calendar.YEAR)
        val m = (now.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = now.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        "$y-$m-$d"
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pointage", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Gestion des présences", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooPrimary),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateForm(todayIso) },
                containerColor = FormulooPrimary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle présence")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Filtres statut ──────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(STATUT_FILTERS) { (code, label) ->
                    FilterChip(
                        selected = statutFilter == code,
                        onClick = { viewModel.setStatutFilter(code) },
                        label = { Text(label, fontSize = 13.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FormulooPrimary,
                            selectedLabelColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = statutFilter == code,
                            borderColor = FormulooOutline,
                            selectedBorderColor = FormulooPrimary,
                        ),
                    )
                }
            }

            // ── Plage de dates ─────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = dateDebut,
                    onValueChange = viewModel::setDateDebut,
                    label = { Text("Depuis", fontSize = 12.sp) },
                    placeholder = { Text("AAAA-MM-JJ", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FormulooPrimary,
                        unfocusedBorderColor = FormulooOutline,
                    ),
                )
                OutlinedTextField(
                    value = dateFin,
                    onValueChange = viewModel::setDateFin,
                    label = { Text("Jusqu'au", fontSize = 12.sp) },
                    placeholder = { Text("AAAA-MM-JJ", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FormulooPrimary,
                        unfocusedBorderColor = FormulooOutline,
                    ),
                )
                TextButton(onClick = viewModel::applyDateFilter, modifier = Modifier.align(Alignment.CenterVertically)) {
                    Text("OK", color = FormulooPrimary, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = FormulooOutline)

            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune présence pour ces filtres.", color = FormulooOnSurfaceVariant)
                }
                is UiState.Success -> {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp)) {
                        items(s.data, key = { it.id }) { presence ->
                            AdminPresenceCard(
                                presence = presence,
                                onArchive = { viewModel.archivePresence(presence.id) },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = FormulooError)
                }
            }
        }
    }

    // ── Dialogue de création ─────────────────────────────────────────────
    if (createForm != null) {
        CreatePresenceDialog(
            form = createForm!!,
            employees = employees,
            onUpdate = viewModel::updateForm,
            onSubmit = viewModel::submitCreate,
            onDismiss = viewModel::closeCreateForm,
        )
    }

    // ── Erreur d'action (archive) ─────────────────────────────────────────
    if (actionError != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearActionError,
            title = { Text("Erreur") },
            text = { Text(actionError!!) },
            confirmButton = {
                TextButton(onClick = viewModel::clearActionError) { Text("OK") }
            },
        )
    }
}

@Composable
private fun AdminPresenceCard(
    presence: Presence,
    onArchive: () -> Unit,
) {
    val (statusColor, statusLabel) = presenceAdminStatusMeta(presence.statut)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(
                initials = presence.employeeInitials,
                size = 42.dp,
                backgroundColor = avatarColorForId(presence.employeeId),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(presence.employeeName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FormulooTextPrimary)
                Text(
                    formatPresenceDate(presence.date),
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                    if (presence.heureArrivee != null || presence.heureDepart != null) {
                        Text(
                            "${presence.heureArrivee ?: "—"} → ${presence.heureDepart ?: "—"}",
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }
                if (presence.heuresTravaillees != null) {
                    Text("${presence.heuresTravaillees} h", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Supprimer", tint = FormulooOnSurfaceVariant)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = FormulooError) },
                        onClick = {
                            menuExpanded = false
                            onArchive()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePresenceDialog(
    form: PresenceCreateForm,
    employees: List<Employee>,
    onUpdate: ((PresenceCreateForm) -> PresenceCreateForm) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saisir une présence", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Employé dropdown
                FormulooDropdownField(
                    value = form.employeeName,
                    onValueChange = { name ->
                        val emp = employees.firstOrNull { it.fullName == name }
                        if (emp != null) onUpdate { it.copy(employeeId = emp.id, employeeName = emp.fullName) }
                    },
                    label = "Employé *",
                    options = employees.map { it.fullName },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Date
                FormulooTextField(
                    value = form.date,
                    onValueChange = { v -> onUpdate { it.copy(date = v) } },
                    label = "Date (AAAA-MM-JJ) *",
                    modifier = Modifier.fillMaxWidth(),
                )

                // Statut
                FormulooDropdownField(
                    value = STATUT_LABELS[form.statut] ?: form.statut,
                    onValueChange = { label ->
                        val code = STATUT_LABELS.entries.firstOrNull { it.value == label }?.key ?: "present"
                        onUpdate { it.copy(statut = code) }
                    },
                    label = "Statut",
                    options = STATUT_OPTIONS.map { STATUT_LABELS[it] ?: it },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Heures
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormulooTextField(
                        value = form.heureArrivee,
                        onValueChange = { v -> onUpdate { it.copy(heureArrivee = v) } },
                        label = "Arrivée",
                        placeholder = "08:00",
                        modifier = Modifier.weight(1f),
                    )
                    FormulooTextField(
                        value = form.heureDepart,
                        onValueChange = { v -> onUpdate { it.copy(heureDepart = v) } },
                        label = "Départ",
                        placeholder = "17:00",
                        modifier = Modifier.weight(1f),
                    )
                }

                // Commentaire
                FormulooTextField(
                    value = form.commentaire,
                    onValueChange = { v -> onUpdate { it.copy(commentaire = v) } },
                    label = "Commentaire",
                    modifier = Modifier.fillMaxWidth(),
                )

                if (form.error != null) {
                    Text(form.error, color = FormulooError, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            if (form.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FormulooPrimary, strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onSubmit) {
                    Text("Enregistrer", color = FormulooPrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

private val avatarPalette = listOf(FormulooPrimary, FormulooBlue, FormulooPurple, FormulooDeepOrange, FormulooGreen, FormulooTeal, FormulooSecondary)
private fun avatarColorForId(id: String): Color = avatarPalette[abs(id.hashCode()) % avatarPalette.size]

private fun presenceAdminStatusMeta(status: PresenceStatus): Pair<Color, String> = when (status) {
    PresenceStatus.PRESENT -> Color(0xFF2E7D32) to "Présent"
    PresenceStatus.ABSENT -> Color(0xFFC62828) to "Absent"
    PresenceStatus.RETARD -> Color(0xFFE65100) to "Retard"
    PresenceStatus.CONGE -> Color(0xFF1565C0) to "Congé"
    PresenceStatus.FERIE -> Color(0xFF6A1B9A) to "Férié"
}

private fun formatPresenceDate(iso: String): String = try {
    val parts = iso.split("-")
    val months = listOf("", "jan.", "fév.", "mar.", "avr.", "mai", "jun.", "jul.", "aoû.", "sep.", "oct.", "nov.", "déc.")
    "${parts[2].toInt()} ${months[parts[1].toInt()]} ${parts[0]}"
} catch (e: Exception) { iso }
