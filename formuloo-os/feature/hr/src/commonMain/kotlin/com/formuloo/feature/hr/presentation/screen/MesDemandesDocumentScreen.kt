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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.FormulooDropdownField
import com.formuloo.core.designsystem.FormulooTextField
import com.formuloo.feature.hr.domain.model.DocumentRequest
import com.formuloo.feature.hr.domain.model.DocumentRequestStatus
import com.formuloo.feature.hr.domain.model.DocumentType
import com.formuloo.feature.hr.presentation.viewmodel.DocumentCreateForm
import com.formuloo.feature.hr.presentation.viewmodel.MesDemandesDocumentViewModel
import org.koin.compose.viewmodel.koinViewModel

private val DOCUMENT_TYPES = listOf(
    "Attestation de travail" to DocumentType.ATTESTATION_TRAVAIL,
    "Attestation de salaire" to DocumentType.ATTESTATION_SALAIRE,
    "Copie bulletin de paie" to DocumentType.BULLETIN_PAIE_COPIE,
)

private val STATUS_FILTERS = listOf(
    "Toutes" to null,
    "En attente" to DocumentRequestStatus.EN_ATTENTE,
    "Approuvées" to DocumentRequestStatus.APPROUVEE,
    "Rejetées" to DocumentRequestStatus.REJETEE,
    "Annulées" to DocumentRequestStatus.ANNULEE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesDemandesDocumentScreen(
    onBack: () -> Unit,
    viewModel: MesDemandesDocumentViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val statutFilter by viewModel.statutFilter.collectAsStateWithLifecycle()
    val showCreateDialog by viewModel.showCreateDialog.collectAsStateWithLifecycle()
    val createForm by viewModel.createForm.collectAsStateWithLifecycle()
    val cancelError by viewModel.cancelError.collectAsStateWithLifecycle()
    val cancellingId by viewModel.cancellingId.collectAsStateWithLifecycle()

    // Local state for inline cancel confirmation
    var confirmCancelId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mes documents", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Demandes de documents RH", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
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
                onClick = viewModel::openCreateDialog,
                containerColor = FormulooPrimary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle demande")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Filtres statut ─────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(STATUS_FILTERS) { (label, statut) ->
                    FilterChip(
                        selected = statutFilter == statut,
                        onClick = { viewModel.setStatutFilter(statut) },
                        label = { Text(label, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FormulooPrimary,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            HorizontalDivider(color = FormulooOutline)

            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aucune demande.", color = FormulooOnSurfaceVariant, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Appuyez sur + pour soumettre une demande.", color = FormulooOnSurfaceVariant, fontSize = 13.sp)
                    }
                }
                is UiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(s.data, key = { it.id }) { request ->
                        DocumentRequestCard(
                            request = request,
                            isCancelling = cancellingId == request.id,
                            onCancel = { confirmCancelId = request.id },
                        )
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = FormulooError)
                }
            }
        }
    }

    // ── Dialogue de création ───────────────────────────────────────────────
    if (showCreateDialog) {
        CreateDocumentDialog(
            form = createForm,
            onTypeChange = viewModel::updateCreateType,
            onMotifChange = viewModel::updateCreateMotif,
            onSubmit = viewModel::submitCreate,
            onDismiss = viewModel::closeCreateDialog,
        )
    }

    // ── Confirmation annulation ────────────────────────────────────────────
    confirmCancelId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmCancelId = null },
            title = { Text("Annuler la demande") },
            text = { Text("Voulez-vous vraiment annuler cette demande ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelRequest(id)
                    confirmCancelId = null
                }) { Text("Annuler la demande", color = FormulooError) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelId = null }) { Text("Garder") }
            },
        )
    }

    // ── Erreur annulation ──────────────────────────────────────────────────
    if (cancelError != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearCancelError,
            title = { Text("Erreur") },
            text = { Text(cancelError ?: "") },
            confirmButton = {
                TextButton(onClick = viewModel::clearCancelError) { Text("OK") }
            },
        )
    }
}

@Composable
private fun DocumentRequestCard(
    request: DocumentRequest,
    isCancelling: Boolean,
    onCancel: () -> Unit,
) {
    val (statusColor, statusLabel) = docStatusMeta(request.statut)
    val (typeIcon, _) = docTypeMeta(request.typeDocument)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── En-tête : type + statut ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(FormulooPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(typeIcon, contentDescription = null, tint = FormulooPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(request.typeDocumentLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FormulooTextPrimary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = statusColor)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Date de soumission ─────────────────────────────────────────
            Text(
                "Soumise le ${formatIsoDate(request.createdAt)}",
                fontSize = 12.sp,
                color = FormulooOnSurfaceVariant,
            )

            // ── Motif de la demande ────────────────────────────────────────
            if (!request.motifDemande.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Motif : ${request.motifDemande}",
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }

            // ── Banderole "Document prêt" si approuvée ─────────────────────
            if (request.statut == DocumentRequestStatus.APPROUVEE && request.hasDocumentData) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Document prêt — traité le ${formatIsoDate(request.traiteeLe ?: "")}", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                }
            }

            // ── Motif de rejet si rejetée ─────────────────────────────────
            if (request.statut == DocumentRequestStatus.REJETEE && !request.motifRejet.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FormulooError.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("Motif de rejet : ${request.motifRejet}", fontSize = 12.sp, color = FormulooError)
                }
            }

            // ── Bouton annuler (seulement si en attente) ──────────────────
            if (request.statut == DocumentRequestStatus.EN_ATTENTE) {
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (isCancelling) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FormulooError)
                    } else {
                        TextButton(onClick = onCancel) {
                            Text("Annuler", color = FormulooError, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateDocumentDialog(
    form: DocumentCreateForm,
    onTypeChange: (DocumentType) -> Unit,
    onMotifChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val typeLabels = DOCUMENT_TYPES.map { it.first }
    val selectedLabel = DOCUMENT_TYPES.firstOrNull { it.second == form.typeDocument }?.first ?: typeLabels.first()

    AlertDialog(
        onDismissRequest = { if (!form.isLoading) onDismiss() },
        title = { Text("Demande de document", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormulooDropdownField(
                    value = selectedLabel,
                    onValueChange = { label ->
                        val type = DOCUMENT_TYPES.firstOrNull { it.first == label }?.second
                            ?: DocumentType.ATTESTATION_TRAVAIL
                        onTypeChange(type)
                    },
                    label = "Type de document",
                    options = typeLabels,
                )
                FormulooTextField(
                    value = form.motifDemande,
                    onValueChange = onMotifChange,
                    label = "Motif (optionnel)",
                    placeholder = "Ex: demande de visa, prêt bancaire…",
                    singleLine = false,
                )
                if (!form.error.isNullOrBlank()) {
                    Text(form.error, color = FormulooError, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSubmit,
                enabled = !form.isLoading,
            ) {
                if (form.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FormulooPrimary)
                } else {
                    Text("Soumettre", color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !form.isLoading) { Text("Annuler") }
        },
    )
}

private fun docStatusMeta(status: DocumentRequestStatus): Pair<Color, String> = when (status) {
    DocumentRequestStatus.EN_ATTENTE -> Color(0xFFE65100) to "En attente"
    DocumentRequestStatus.APPROUVEE -> Color(0xFF2E7D32) to "Approuvée"
    DocumentRequestStatus.REJETEE -> Color(0xFFC62828) to "Rejetée"
    DocumentRequestStatus.ANNULEE -> Color(0xFF757575) to "Annulée"
}

private fun docTypeMeta(type: DocumentType): Pair<ImageVector, String> = when (type) {
    DocumentType.ATTESTATION_TRAVAIL -> Icons.Filled.Article to "Attestation de travail"
    DocumentType.ATTESTATION_SALAIRE -> Icons.Filled.Description to "Attestation de salaire"
    DocumentType.BULLETIN_PAIE_COPIE -> Icons.Filled.Receipt to "Copie bulletin de paie"
}

private fun formatIsoDate(iso: String): String {
    // Parses "YYYY-MM-DDThh:mm:ss..." → "DD/MM/YYYY"
    val parts = iso.substringBefore("T").split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
}
