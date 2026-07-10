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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextField
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.InitialsAvatar
import com.formuloo.feature.hr.domain.model.DocumentRequest
import com.formuloo.feature.hr.domain.model.DocumentRequestStatus
import com.formuloo.feature.hr.presentation.viewmodel.DemandesDocumentRHViewModel
import org.koin.compose.viewmodel.koinViewModel

private data class StatutFilterOption(
    val statut: DocumentRequestStatus?,
    val label: String,
)

private val statutOptions = listOf(
    StatutFilterOption(DocumentRequestStatus.EN_ATTENTE, "En attente"),
    StatutFilterOption(DocumentRequestStatus.APPROUVEE, "Approuvées"),
    StatutFilterOption(DocumentRequestStatus.REJETEE, "Rejetées"),
    StatutFilterOption(DocumentRequestStatus.ANNULEE, "Annulées"),
    StatutFilterOption(null, "Toutes"),
)

private val rhAvatarPalette = listOf(
    FormulooPrimary,
    Color(0xFF6366F1),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF3B82F6),
)

private fun rhAvatarColor(id: String): Color =
    rhAvatarPalette[kotlin.math.abs(id.hashCode()) % rhAvatarPalette.size]

private fun statutColors(statut: DocumentRequestStatus): Pair<Color, Color> = when (statut) {
    DocumentRequestStatus.EN_ATTENTE -> Color(0xFFFFF3CD) to Color(0xFF856404)
    DocumentRequestStatus.APPROUVEE -> FormulooMint to FormulooPrimary
    DocumentRequestStatus.REJETEE -> Color(0xFFFFE8E8) to FormulooError
    DocumentRequestStatus.ANNULEE -> Color(0xFFF5F5F5) to FormulooOnSurfaceVariant
}

private fun formatDate(iso: String): String = try {
    val parts = iso.substring(0, 10).split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) { iso }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemandesDocumentRHScreen(
    onBack: () -> Unit,
    viewModel: DemandesDocumentRHViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val statutFilter by viewModel.statutFilter.collectAsStateWithLifecycle()
    val processingId by viewModel.processingId.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val rejectTargetId by viewModel.rejectTargetId.collectAsStateWithLifecycle()
    val rejectMotif by viewModel.rejectMotif.collectAsStateWithLifecycle()
    val rejectLoading by viewModel.rejectLoading.collectAsStateWithLifecycle()

    // ── Dialogue de rejet ────────────────────────────────────────────────
    if (rejectTargetId != null) {
        AlertDialog(
            onDismissRequest = { if (!rejectLoading) viewModel.closeRejectDialog() },
            title = { Text("Motif de rejet", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Veuillez expliquer le motif du rejet à l'employé (min. 5 caractères).",
                        fontSize = 13.sp,
                        color = FormulooOnSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    FormulooTextField(
                        value = rejectMotif,
                        onValueChange = viewModel::updateRejectMotif,
                        label = "Motif",
                        singleLine = false,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::submitReject,
                    enabled = rejectMotif.trim().length >= 5 && !rejectLoading,
                ) {
                    if (rejectLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FormulooError)
                    } else {
                        Text("Rejeter", color = FormulooError, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeRejectDialog, enabled = !rejectLoading) {
                    Text("Annuler")
                }
            },
        )
    }

    // ── Dialogue d'erreur d'action ────────────────────────────────────────
    if (actionError != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearActionError,
            title = { Text("Erreur") },
            text = { Text(actionError ?: "") },
            confirmButton = {
                TextButton(onClick = viewModel::clearActionError) { Text("OK") }
            },
        )
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Demandes de documents", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FormulooTextPrimary)
                        Text("Traitement RH", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = FormulooTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualiser", tint = FormulooTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // ── Filtres statut ────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(statutOptions) { option ->
                    FilterChip(
                        selected = statutFilter == option.statut,
                        onClick = { viewModel.setStatutFilter(option.statut) },
                        label = { Text(option.label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FormulooPrimary,
                            selectedLabelColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = statutFilter == option.statut,
                            borderColor = FormulooOutline,
                            selectedBorderColor = FormulooPrimary,
                        ),
                    )
                }
            }

            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (statutFilter == DocumentRequestStatus.EN_ATTENTE)
                                "Aucune demande en attente"
                            else
                                "Aucune demande dans cette catégorie",
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }
                is UiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            "${s.data.size} DEMANDE${if (s.data.size > 1) "S" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FormulooOnSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    items(s.data, key = { it.id }) { request ->
                        DocumentRequestRHCard(
                            request = request,
                            isProcessing = processingId == request.id,
                            onApprove = { viewModel.approve(request.id) },
                            onReject = { viewModel.openRejectDialog(request.id) },
                        )
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = FormulooError)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun DocumentRequestRHCard(
    request: DocumentRequest,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val employeeId = request.employeeId ?: request.id
    val employeeName = request.employeeName ?: "Employé inconnu"
    val initials = request.employeeInitials ?: employeeName.split(" ")
        .filter { it.isNotBlank() }
        .joinToString("") { it.first().uppercaseChar().toString() }
        .take(2)
        .ifBlank { "?" }

    val (statusBg, statusFg) = statutColors(request.statut)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(
                    initials = initials,
                    size = 40.dp,
                    backgroundColor = rhAvatarColor(employeeId),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(employeeName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FormulooTextPrimary)
                    Text(request.typeDocumentLabel, fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                }
                // Badge statut
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(request.statutLabel, fontSize = 11.sp, color = statusFg, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Demandé le ${formatDate(request.createdAt)}",
                fontSize = 12.sp,
                color = FormulooOnSurfaceVariant,
            )

            if (!request.motifDemande.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Motif : ${request.motifDemande}",
                    fontSize = 12.sp,
                    color = FormulooTextPrimary,
                )
            }

            if (!request.motifRejet.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Rejeté : ${request.motifRejet}",
                    fontSize = 12.sp,
                    color = FormulooError,
                )
            }

            if (request.traiteeLe != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Traité le ${formatDate(request.traiteeLe)}",
                    fontSize = 11.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }

            if (request.statut == DocumentRequestStatus.EN_ATTENTE) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = FormulooPrimary)
                    } else {
                        TextButton(onClick = onReject) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = FormulooError, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Rejeter", color = FormulooError, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onApprove) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = FormulooPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Approuver", color = FormulooPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
