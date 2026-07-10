package com.formuloo.feature.compta.presentation.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.network.dto.compta.EcritureDto
import com.formuloo.core.network.dto.compta.JournalDto
import com.formuloo.feature.compta.presentation.viewmodel.LigneForm
import com.formuloo.feature.compta.presentation.viewmodel.SaisieUiState

@Composable
internal fun SaisieTab(
    state: SaisieUiState,
    onSelectJournal: (Int) -> Unit,
    onSetDate: (String) -> Unit,
    onSetLibelle: (String) -> Unit,
    onSetReferencePiece: (String) -> Unit,
    onUpdateLigneNumero: (Int, String) -> Unit,
    onUpdateLigneLibelle: (Int, String) -> Unit,
    onUpdateLigneDebit: (Int, String) -> Unit,
    onUpdateLigneCredit: (Int, String) -> Unit,
    onAddLigne: () -> Unit,
    onRemoveLigne: (Int) -> Unit,
    onSaveAsBrouillon: () -> Unit,
    onRequestValidation: () -> Unit,
    onDismissValidationDialog: () -> Unit,
    onConfirmValidation: () -> Unit,
    onDeleteBrouillon: () -> Unit,
    onDismissSuccess: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FormulooPrimary)
        }

        state.loadError != null -> Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.loadError, color = FormulooError, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) { Text("Réessayer", color = FormulooPrimary) }
            }
        }

        state.submitSuccess -> SaisieSuccessState(
            ecritureId = state.successId,
            onNouvelleEcriture = onDismissSuccess,
        )

        else -> {
            SaisieForm(
                state = state,
                onSelectJournal = onSelectJournal,
                onSetDate = onSetDate,
                onSetLibelle = onSetLibelle,
                onSetReferencePiece = onSetReferencePiece,
                onUpdateLigneNumero = onUpdateLigneNumero,
                onUpdateLigneLibelle = onUpdateLigneLibelle,
                onUpdateLigneDebit = onUpdateLigneDebit,
                onUpdateLigneCredit = onUpdateLigneCredit,
                onAddLigne = onAddLigne,
                onRemoveLigne = onRemoveLigne,
                onSaveAsBrouillon = onSaveAsBrouillon,
                onRequestValidation = onRequestValidation,
                onDeleteBrouillon = onDeleteBrouillon,
            )

            if (state.showValidationDialog) {
                ValidationConfirmDialog(
                    ecritureId = state.brouillonId,
                    isValidating = state.isValidating,
                    onConfirm = onConfirmValidation,
                    onDismiss = onDismissValidationDialog,
                )
            }
        }
    }
}

// ── État succès ───────────────────────────────────────────────────────────────

@Composable
private fun SaisieSuccessState(ecritureId: Int?, onNouvelleEcriture: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(FormulooMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = FormulooPrimary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Écriture validée", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FormulooTextPrimary)
            Spacer(Modifier.height(8.dp))
            if (ecritureId != null) {
                Text(
                    "Écriture #$ecritureId comptabilisée définitivement.",
                    fontSize = 13.sp, color = FormulooOnSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onNouvelleEcriture,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
            ) {
                Text("Nouvelle écriture", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Dialog de confirmation de validation ──────────────────────────────────────

@Composable
private fun ValidationConfirmDialog(
    ecritureId: Int?,
    isValidating: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isValidating) onDismiss() },
        icon = {
            Icon(Icons.Filled.Warning, null, tint = FormulooSecondary, modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Confirmer la validation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Text(
                    "Cette action est irréversible. Une fois validée, l'écriture #${ecritureId ?: "?"} " +
                        "ne pourra plus être modifiée ni supprimée.",
                    fontSize = 14.sp,
                    color = FormulooOnSurfaceVariant,
                )
                if (isValidating) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FormulooPrimary)
                        Text("Validation en cours…", fontSize = 13.sp, color = FormulooPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isValidating,
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
            ) {
                Text("Valider définitivement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isValidating) {
                Text("Annuler", color = FormulooOnSurfaceVariant)
            }
        },
    )
}

// ── Formulaire principal ──────────────────────────────────────────────────────

@Composable
private fun SaisieForm(
    state: SaisieUiState,
    onSelectJournal: (Int) -> Unit,
    onSetDate: (String) -> Unit,
    onSetLibelle: (String) -> Unit,
    onSetReferencePiece: (String) -> Unit,
    onUpdateLigneNumero: (Int, String) -> Unit,
    onUpdateLigneLibelle: (Int, String) -> Unit,
    onUpdateLigneDebit: (Int, String) -> Unit,
    onUpdateLigneCredit: (Int, String) -> Unit,
    onAddLigne: () -> Unit,
    onRemoveLigne: (Int) -> Unit,
    onSaveAsBrouillon: () -> Unit,
    onRequestValidation: () -> Unit,
    onDeleteBrouillon: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FormulooBackground),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── État brouillon sauvegardé ──────────────────────────────────────
        if (state.brouillonId != null) {
            item {
                BrouillonBanner(
                    brouillonId = state.brouillonId,
                    isValidating = state.isValidating,
                    isDeleting = state.isDeleting,
                    onRequestValidation = onRequestValidation,
                    onDelete = onDeleteBrouillon,
                )
            }
        }

        // ── Journal dropdown (bare, sans carte) ───────────────────────────
        item {
            JournalField(
                journaux = state.journaux,
                selectedId = state.selectedJournalId,
                onSelect = onSelectJournal,
            )
        }

        // ── Date + Réf + Libellé (bare, sans enveloppe de carte) ──────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SaisieField(
                        label = "Date",
                        required = true,
                        value = state.date,
                        hint = "AAAA-MM-JJ",
                        keyboardType = KeyboardType.Number,
                        onValueChange = onSetDate,
                        modifier = Modifier.weight(1f),
                    )
                    SaisieField(
                        label = "Réf. pièce",
                        value = state.referencePiece,
                        hint = "N° facture…",
                        onValueChange = onSetReferencePiece,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                SaisieField(
                    label = "Libellé de l'écriture",
                    required = true,
                    value = state.libelle,
                    hint = "Ex : Règlement facture client INV-001",
                    onValueChange = onSetLibelle,
                )
            }
        }

        // ── Section lignes title ───────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "LIGNES D'ÉCRITURE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = FormulooOnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(FormulooMint)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "${state.lignes.size} ligne${if (state.lignes.size > 1) "s" else ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FormulooPrimary,
                    )
                }
            }
        }

        // ── Lignes ─────────────────────────────────────────────────────────
        items(state.lignes, key = { it.localId }) { ligne ->
            LigneFormCard(
                ligne = ligne,
                canDelete = state.lignes.size > 2,
                onUpdateNumero = { onUpdateLigneNumero(ligne.localId, it) },
                onUpdateLibelle = { onUpdateLigneLibelle(ligne.localId, it) },
                onUpdateDebit = { onUpdateLigneDebit(ligne.localId, it) },
                onUpdateCredit = { onUpdateLigneCredit(ligne.localId, it) },
                onRemove = { onRemoveLigne(ligne.localId) },
            )
        }

        // ── Ajouter une ligne (bordure pointillée) ─────────────────────────
        item {
            val dashColor = FormulooPrimary.copy(alpha = 0.45f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = dashColor,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                            ),
                            cornerRadius = CornerRadius(10.dp.toPx()),
                        )
                    }
                    .clickable(onClick = onAddLigne)
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, null, tint = FormulooPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ajouter une ligne", color = FormulooPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        // ── Bannière d'équilibre ───────────────────────────────────────────
        item {
            BalanceBanner(
                totalDebit = state.totalDebit,
                totalCredit = state.totalCredit,
                isEquilibree = state.isEquilibree,
            )
        }

        // ── Récapitulatif totaux ───────────────────────────────────────────
        item {
            TotauxRow(totalDebit = state.totalDebit, totalCredit = state.totalCredit)
        }

        // ── Bannière CRM ───────────────────────────────────────────────────
        item {
            CrmLinkBanner(referencePiece = state.referencePiece)
        }

        // ── Erreur + Bouton principal ──────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (state.submitError != null) {
                    Text(
                        state.submitError,
                        color = FormulooError,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FormulooError.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (state.brouillonId == null) {
                    Button(
                        onClick = onSaveAsBrouillon,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FormulooPrimary,
                            disabledContainerColor = FormulooOutline,
                        ),
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Enregistrement…", fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Enregistrer en brouillon", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── Écritures récentes ─────────────────────────────────────────────
        if (state.ecritures.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Écritures récentes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FormulooTextPrimary)
                }
            }

            items(state.ecritures.take(10), key = { "ecriture_${it.id}" }) { ecriture ->
                EcritureRecente(ecriture = ecriture)
            }
        }
    }
}

// ── Bannière brouillon sauvegardé ─────────────────────────────────────────────

@Composable
private fun BrouillonBanner(
    brouillonId: Int,
    isValidating: Boolean,
    isDeleting: Boolean,
    onRequestValidation: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSecondaryBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, FormulooSecondary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(FormulooSecondary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("BROUILLON", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FormulooSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Écriture #$brouillonId sauvegardée",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FormulooTextPrimary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Elle n'est pas encore comptabilisée. Validez-la ou supprimez-la.",
                fontSize = 12.sp,
                color = FormulooOnSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRequestValidation,
                    enabled = !isValidating && !isDeleting,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Valider", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isValidating && !isDeleting,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FormulooError.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = FormulooError, strokeWidth = 2.dp)
                    } else {
                        Text("Supprimer", fontSize = 13.sp, color = FormulooError)
                    }
                }
            }
        }
    }
}

// ── Journal dropdown ──────────────────────────────────────────────────────────

@Composable
private fun JournalField(
    journaux: List<JournalDto>,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = journaux.find { it.id == selectedId }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Journal", fontSize = 11.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(" *", fontSize = 11.sp, color = FormulooError, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(6.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(FormulooSurface)
                    .border(
                        1.dp,
                        if (selected != null) FormulooPrimary.copy(alpha = 0.4f) else FormulooOutline,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(FormulooPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(selected.code.take(2), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${selected.code} — ${selected.libelle}",
                        fontSize = 14.sp, color = FormulooTextPrimary,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        if (journaux.isEmpty()) "Aucun journal disponible" else "Sélectionner un journal…",
                        fontSize = 14.sp, color = FormulooOnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                    )
                }
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = FormulooOnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                journaux.forEach { journal ->
                    val isSelected = journal.id == selectedId
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.25f) else FormulooMint,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        journal.code.take(2),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else FormulooPrimary,
                                    )
                                }
                                Column {
                                    Text(
                                        journal.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else FormulooTextPrimary,
                                    )
                                    Text(
                                        journal.libelle,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else FormulooOnSurfaceVariant,
                                    )
                                }
                            }
                        },
                        onClick = { onSelect(journal.id); expanded = false },
                        modifier = if (isSelected) Modifier.background(Color(0xFF1A56A0)) else Modifier,
                    )
                }
            }
        }
    }
}

// ── Champ de saisie générique ─────────────────────────────────────────────────

@Composable
private fun SaisieField(
    label: String,
    value: String,
    hint: String = "",
    required: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 11.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium)
            if (required) {
                Text(" *", fontSize = 11.sp, color = FormulooError, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(fontSize = 14.sp, color = FormulooTextPrimary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooSurface, RoundedCornerShape(8.dp))
                .border(1.dp, FormulooOutline, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, fontSize = 14.sp, color = FormulooOnSurfaceVariant.copy(alpha = 0.6f))
                }
                inner()
            },
        )
    }
}

// ── Ligne comptable ───────────────────────────────────────────────────────────

@Composable
private fun LigneFormCard(
    ligne: LigneForm,
    canDelete: Boolean,
    onUpdateNumero: (String) -> Unit,
    onUpdateLibelle: (String) -> Unit,
    onUpdateDebit: (String) -> Unit,
    onUpdateCredit: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Badge N° compte — fond blanc, bordure fine, texte teal gras
                Column(modifier = Modifier.width(90.dp)) {
                    BasicTextField(
                        value = ligne.compteNumero,
                        onValueChange = onUpdateNumero,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ligne.compteError != null) FormulooError else FormulooPrimary,
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (ligne.compteError != null) FormulooError.copy(alpha = 0.06f) else FormulooSurface,
                                RoundedCornerShape(6.dp),
                            )
                            .border(
                                1.dp,
                                when {
                                    ligne.compteError != null -> FormulooError
                                    ligne.compteId != null -> FormulooPrimary.copy(alpha = 0.5f)
                                    else -> FormulooOutline
                                },
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        decorationBox = { inner ->
                            if (ligne.compteNumero.isEmpty()) {
                                Text("N° compte", fontSize = 11.sp, color = FormulooOnSurfaceVariant.copy(alpha = 0.6f))
                            }
                            inner()
                        },
                    )
                    val subText = ligne.compteError ?: ligne.compteLibelle
                    val subColor = if (ligne.compteError != null) FormulooError else FormulooOnSurfaceVariant
                    if (subText.isNotEmpty()) {
                        Text(
                            text = subText,
                            fontSize = 9.sp,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                // Libellé ligne
                BasicTextField(
                    value = ligne.libelle,
                    onValueChange = onUpdateLibelle,
                    textStyle = TextStyle(fontSize = 13.sp, color = FormulooTextPrimary),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .background(FormulooBackground, RoundedCornerShape(6.dp))
                        .border(1.dp, FormulooOutline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    decorationBox = { inner ->
                        if (ligne.libelle.isEmpty()) {
                            Text("Libellé ligne…", fontSize = 13.sp, color = FormulooOnSurfaceVariant.copy(alpha = 0.6f))
                        }
                        inner()
                    },
                )

                if (canDelete) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, null, tint = FormulooError, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Spacer(Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // DÉBIT + CRÉDIT — fond blanc, bordure grise, labels gris uppercase
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountField(
                    label = "DÉBIT",
                    value = ligne.debit,
                    disabled = ligne.hasCredit,
                    onValueChange = onUpdateDebit,
                    modifier = Modifier.weight(1f),
                )
                AmountField(
                    label = "CRÉDIT",
                    value = ligne.credit,
                    disabled = ligne.hasDebit,
                    onValueChange = onUpdateCredit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    disabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.alpha(if (disabled) 0.35f else 1f)) {
        Text(label, fontSize = 10.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        BasicTextField(
            value = value,
            onValueChange = if (disabled) { _ -> } else onValueChange,
            readOnly = disabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooTextPrimary,
                textAlign = TextAlign.End,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooSurface, RoundedCornerShape(6.dp))
                .border(1.dp, FormulooOutline, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text("0", fontSize = 14.sp, color = FormulooOnSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
                inner()
            },
        )
    }
}

// ── Bannière d'équilibre (icône badge carré arrondi, sans résumé D/C) ─────────

@Composable
private fun BalanceBanner(totalDebit: Double, totalCredit: Double, isEquilibree: Boolean) {
    val bgColor = if (isEquilibree) FormulooMint else FormulooSecondaryBg
    val iconBgColor = if (isEquilibree) FormulooPrimary else FormulooSecondary
    val titleColor = if (isEquilibree) FormulooPrimary else FormulooSecondary
    val ecart = kotlin.math.abs(totalDebit - totalCredit)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isEquilibree) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Column {
            Text(
                if (isEquilibree) "Écriture équilibrée" else "Écriture déséquilibrée",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = titleColor,
            )
            if (!isEquilibree && ecart > 0.01) {
                Text(
                    "Écart : ${formatAmount(ecart)} XAF",
                    fontSize = 12.sp,
                    color = FormulooSecondary.copy(alpha = 0.8f),
                )
            } else if (isEquilibree && totalDebit > 0) {
                Text(
                    "Montant équilibré : ${formatAmount(totalDebit)} XAF",
                    fontSize = 12.sp,
                    color = FormulooPrimary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ── Récapitulatif totaux ──────────────────────────────────────────────────────

@Composable
private fun TotauxRow(totalDebit: Double, totalCredit: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Total débit", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
            Text(formatAmount(totalDebit), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Total crédit", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
            Text(formatAmount(totalCredit), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
        }
    }
}

// ── Bannière de liaison CRM ───────────────────────────────────────────────────

@Composable
private fun CrmLinkBanner(referencePiece: String) {
    if (referencePiece.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(FormulooMint, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = FormulooPrimary,
            modifier = Modifier.size(18.dp).padding(top = 1.dp),
        )
        Column {
            Text(
                "Lié à la facture / fournisseur",
                fontSize = 12.sp,
                color = FormulooOnSurfaceVariant,
            )
            Text(
                referencePiece,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FormulooPrimary,
            )
        }
    }
}

// ── Écritures récentes ────────────────────────────────────────────────────────

@Composable
private fun EcritureRecente(ecriture: EcritureDto) {
    val isValidee = ecriture.statut == "VALIDEE"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(FormulooSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                ecriture.libelle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = FormulooTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(ecriture.dateEcriture, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
                Text("·", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
                Text(ecriture.journalCode, fontSize = 11.sp, color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
                if (ecriture.referencePiece.isNotBlank()) {
                    Text("·", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
                    Text(ecriture.referencePiece, fontSize = 11.sp, color = FormulooOnSurfaceVariant, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatAmount(ecriture.totalDebit.toDoubleOrNull() ?: 0.0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FormulooTextPrimary,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isValidee) FormulooMint else FormulooSecondaryBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    if (isValidee) "Validée" else "Brouillon",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isValidee) FormulooPrimary else FormulooSecondary,
                )
            }
        }
    }
    HorizontalDivider(color = FormulooOutline.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}
