package com.formuloo.feature.compta.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.formuloo.feature.compta.presentation.viewmodel.JournauxUiState

// Couleur du badge initiales selon le type de journal
private fun journalBadgeColor(type: String): Color = when (type) {
    "VENTES", "PAIE" -> Color(0xFF1A6B5A)  // teal (FormulooPrimary)
    "ACHATS" -> Color(0xFFE97316)           // orange
    "BANQUE" -> Color(0xFF1565C0)           // bleu
    "CAISSE" -> Color(0xFF7B1FA2)           // violet
    "OD" -> Color(0xFF78909C)               // gris ardoise
    else -> Color(0xFF1A6B5A)
}

private fun formatCompact(value: Double): String = when {
    value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000)} G"
    value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)} M"
    value >= 1_000 -> "${"%.1f".format(value / 1_000)} K"
    else -> value.toLong().toString()
}

private val JOURNAL_TYPES = listOf("VENTES", "ACHATS", "BANQUE", "CAISSE", "OD")

private fun typeLabel(type: String): String = when (type) {
    "VENTES" -> "Ventes"
    "ACHATS" -> "Achats"
    "BANQUE" -> "Banque"
    "CAISSE" -> "Caisse"
    "OD" -> "Opérations diverses"
    else -> type
}

@Composable
internal fun JournauxTab(
    state: JournauxUiState,
    onRetry: () -> Unit,
    onSetTypeFilter: (String?) -> Unit,
    onToggleJournal: (Int) -> Unit,
    onShowCreateDialog: () -> Unit,
    onDismissCreateDialog: () -> Unit,
    onSetCreateCode: (String) -> Unit,
    onSetCreateLibelle: (String) -> Unit,
    onSetCreateType: (String) -> Unit,
    onCreateJournal: () -> Unit,
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

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(FormulooBackground),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // ── Cartes statistiques ────────────────────────────────────
                item {
                    JournauxStatsRow(state)
                }

                // ── Liste des journaux ─────────────────────────────────────
                if (state.journaux.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Aucun journal disponible.",
                                color = FormulooOnSurfaceVariant,
                                fontSize = 14.sp,
                            )
                        }
                    }
                } else {
                    items(state.journaux, key = { it.id }) { journal ->
                        JournalCard(
                            journal = journal,
                            isExpanded = state.expandedJournalId == journal.id,
                            isLoadingEcritures = state.loadingEcrituresForJournal == journal.id,
                            ecritures = state.journalEcritures[journal.id],
                            onToggle = { onToggleJournal(journal.id) },
                        )
                    }
                }

                // ── Bannière info ──────────────────────────────────────────
                item {
                    JournauxInfoBanner()
                }
            }

            if (state.showCreateDialog) {
                CreateJournalDialog(
                    code = state.createCode,
                    libelle = state.createLibelle,
                    type = state.createType,
                    isCreating = state.isCreating,
                    error = state.createError,
                    onSetCode = onSetCreateCode,
                    onSetLibelle = onSetCreateLibelle,
                    onSetType = onSetCreateType,
                    onCreate = onCreateJournal,
                    onDismiss = onDismissCreateDialog,
                )
            }
        }
    }
}

// ── Cartes statistiques ───────────────────────────────────────────────────────

@Composable
private fun JournauxStatsRow(state: JournauxUiState) {
    val totalEcritures = state.journaux.sumOf { it.nbEcritures }
    val annee = state.exerciceAnnee

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        JournauxStatCard(
            label = "Journaux actifs",
            value = state.journaux.size.toString(),
            subLabel = "obligatoires SYSCOHADA",
            iconBg = FormulooMint,
            iconTint = FormulooPrimary,
            icon = Icons.Filled.AccountBalance,
        )
        JournauxStatCard(
            label = "Écritures cumulées",
            value = totalEcritures.toString(),
            subLabel = if (annee != null) "exercice $annee" else "exercice en cours",
            iconBg = Color(0xFFDCEEFB),
            iconTint = Color(0xFF1565C0),
            icon = Icons.Filled.History,
        )
        JournauxStatCard(
            label = "Mouvements",
            value = formatCompact(totalEcritures.toDouble() * 2),
            subLabel = "débit = crédit",
            iconBg = FormulooMint,
            iconTint = FormulooPrimary,
            icon = Icons.Filled.SwapHoriz,
        )
    }
}

@Composable
private fun JournauxStatCard(
    label: String,
    value: String,
    subLabel: String,
    iconBg: Color,
    iconTint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        modifier = Modifier.width(156.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = FormulooTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 12.sp, color = FormulooOnSurfaceVariant, maxLines = 1)
            Text(subLabel, fontSize = 10.sp, color = FormulooOnSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
        }
    }
}

// ── Carte journal ─────────────────────────────────────────────────────────────

@Composable
private fun JournalCard(
    journal: JournalDto,
    isExpanded: Boolean,
    isLoadingEcritures: Boolean,
    ecritures: List<EcritureDto>?,
    onToggle: () -> Unit,
) {
    val badgeColor = journalBadgeColor(journal.type)
    val initials = journal.code.take(2).uppercase()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FormulooSurface),
    ) {
        // ── Ligne principale ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Badge initiales carré arrondi
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    journal.libelle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = FormulooTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${journal.nbEcritures} écriture${if (journal.nbEcritures != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = FormulooOnSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        // ── Section écritures expandée (comportement existant préservé) ────
        if (isExpanded) {
            HorizontalDivider(color = FormulooOutline.copy(alpha = 0.4f), thickness = 0.5.dp)
            when {
                isLoadingEcritures -> Box(
                    Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = badgeColor)
                }

                ecritures == null || ecritures.isEmpty() -> Box(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Aucune écriture dans ce journal.", fontSize = 13.sp, color = FormulooOnSurfaceVariant)
                }

                else -> Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    ecritures.take(15).forEach { ecriture ->
                        EcritureRow(ecriture = ecriture)
                        HorizontalDivider(
                            color = FormulooOutline.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }
                    if (ecritures.size > 15) {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "+ ${ecritures.size - 15} autres écritures",
                                fontSize = 12.sp,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EcritureRow(ecriture: EcritureDto) {
    val isValidee = ecriture.statut == "VALIDEE"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                ecriture.libelle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = FormulooTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(ecriture.dateEcriture, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
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
                fontWeight = FontWeight.SemiBold,
                color = FormulooTextPrimary,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isValidee) FormulooMint else FormulooSecondaryBg)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
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
}

// ── Bannière info ─────────────────────────────────────────────────────────────

@Composable
private fun JournauxInfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FormulooMint)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = FormulooPrimary,
            modifier = Modifier.size(18.dp).padding(top = 1.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Journaux auxiliaires SYSCOHADA",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "Les journaux auxiliaires alimentent le journal général. Chaque écriture doit être équilibrée (débit = crédit) selon les règles OHADA.",
                fontSize = 12.sp,
                color = FormulooPrimary.copy(alpha = 0.75f),
            )
        }
    }
}

// ── Dialogue de création ──────────────────────────────────────────────────────

@Composable
private fun CreateJournalDialog(
    code: String,
    libelle: String,
    type: String,
    isCreating: Boolean,
    error: String?,
    onSetCode: (String) -> Unit,
    onSetLibelle: (String) -> Unit,
    onSetType: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        icon = {
            Icon(Icons.Filled.Edit, null, tint = FormulooPrimary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("Nouveau journal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CreateDialogField(
                    label = "Code *",
                    value = code,
                    hint = "Ex : BNQ2",
                    enabled = !isCreating,
                    onValueChange = onSetCode,
                )
                CreateDialogField(
                    label = "Libellé *",
                    value = libelle,
                    hint = "Ex : Banque Secondaire",
                    enabled = !isCreating,
                    onValueChange = onSetLibelle,
                )
                TypeDropdown(selected = type, enabled = !isCreating, onSelect = onSetType)

                if (error != null) {
                    Text(
                        error,
                        color = FormulooError,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FormulooError.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                    )
                }

                if (isCreating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FormulooPrimary)
                        Text("Création en cours…", fontSize = 13.sp, color = FormulooPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Annuler", color = FormulooOnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun CreateDialogField(
    label: String,
    value: String,
    hint: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(label, fontSize = 11.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = if (enabled) onValueChange else { _ -> },
            readOnly = !enabled,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = FormulooTextPrimary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooBackground, RoundedCornerShape(8.dp))
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

@Composable
private fun TypeDropdown(selected: String, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val badgeColor = journalBadgeColor(selected)
    Column {
        Text("Type *", fontSize = 11.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { if (enabled) expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FormulooOutline),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(badgeColor),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    typeLabel(selected),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                    fontSize = 14.sp,
                    color = FormulooTextPrimary,
                )
                Icon(Icons.Filled.ArrowDropDown, null, tint = FormulooOnSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                JOURNAL_TYPES.forEach { t ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(journalBadgeColor(t)),
                                )
                                Text(typeLabel(t), fontSize = 14.sp)
                            }
                        },
                        onClick = {
                            onSelect(t)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
