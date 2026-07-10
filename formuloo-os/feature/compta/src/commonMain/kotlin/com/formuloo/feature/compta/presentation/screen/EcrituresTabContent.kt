package com.formuloo.feature.compta.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.formuloo.core.network.dto.compta.LigneEcritureDto
import com.formuloo.feature.compta.presentation.viewmodel.EcrituresListUiState

// ── Colour helpers ────────────────────────────────────────────────────────────

private fun journalCodeBadgeColor(code: String): Color = when (code.uppercase()) {
    "VT", "PAIE" -> Color(0xFF1A6B5A)
    "AC" -> Color(0xFFE97316)
    "BQ" -> Color(0xFF1565C0)
    "CA" -> Color(0xFF7B1FA2)
    "OD" -> Color(0xFF78909C)
    else -> Color(0xFF1A6B5A)
}

private fun formatCompact(value: Double): String = when {
    value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000)} G"
    value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)} M"
    value >= 1_000 -> "${"%.1f".format(value / 1_000)} K"
    else -> value.toLong().toString()
}

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
internal fun EcrituresTab(
    state: EcrituresListUiState,
    onRetry: () -> Unit,
    onSetJournalFilter: (String?) -> Unit,
    onToggleEcriture: (Int) -> Unit,
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FormulooPrimary)
        }
        state.loadError != null -> Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.loadError, color = FormulooError, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) { Text("Réessayer", color = FormulooPrimary) }
            }
        }
        else -> EcrituresContent(
            state = state,
            onSetJournalFilter = onSetJournalFilter,
            onToggleEcriture = onToggleEcriture,
        )
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun EcrituresContent(
    state: EcrituresListUiState,
    onSetJournalFilter: (String?) -> Unit,
    onToggleEcriture: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FormulooBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            EcrituresStatsRow(state = state)
        }
        item {
            EcrituresFilterChips(
                selectedCode = state.journalCodeFilter,
                availableCodes = state.distinctJournalCodes,
                onSelect = onSetJournalFilter,
            )
        }
        if (state.filteredEcritures.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Aucune écriture trouvée",
                        color = FormulooOnSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
        } else {
            items(state.filteredEcritures, key = { it.id }) { ecriture ->
                EcritureCard(
                    ecriture = ecriture,
                    isExpanded = state.expandedEcritureId == ecriture.id,
                    onToggle = { onToggleEcriture(ecriture.id) },
                )
            }
        }
    }
}

// ── Stat cards row ────────────────────────────────────────────────────────────

@Composable
private fun EcrituresStatsRow(state: EcrituresListUiState) {
    val monthLabel = if (state.exerciceAnnee != null) "Exercice ${state.exerciceAnnee}" else "Ce mois"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EcritureStatCard(
            icon = Icons.Filled.History,
            iconTint = FormulooPrimary,
            iconBg = FormulooMint,
            value = "${state.ecritures.size}",
            label = "Écritures du mois",
            sublabel = monthLabel,
        )
        EcritureStatCard(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = FormulooPrimary,
            iconBg = FormulooMint,
            value = "${formatCompact(state.totalDebit)} F",
            label = "Total débit",
            sublabel = "mouvements",
        )
        EcritureStatCard(
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            iconTint = Color(0xFF1565C0),
            iconBg = Color(0xFFE3F2FD),
            value = "${formatCompact(state.totalCredit)} F",
            label = "Total crédit",
            sublabel = "équilibré",
        )
        EcritureStatCard(
            icon = Icons.Filled.History,
            iconTint = FormulooSecondary,
            iconBg = FormulooSecondaryBg,
            value = "${state.nbEnAttente}",
            label = "En attente",
            sublabel = "à valider",
        )
    }
}

@Composable
private fun EcritureStatCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    value: String,
    label: String,
    sublabel: String,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FormulooSurface)
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = FormulooOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sublabel, fontSize = 10.sp, color = FormulooOnSurfaceVariant.copy(alpha = 0.7f), maxLines = 1)
    }
}

// ── Filter chips ──────────────────────────────────────────────────────────────

@Composable
private fun EcrituresFilterChips(
    selectedCode: String?,
    availableCodes: List<String>,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EcritureFilterChip(label = "Tous", selected = selectedCode == null, onClick = { onSelect(null) })
        availableCodes.forEach { code ->
            EcritureFilterChip(
                label = code,
                selected = selectedCode == code,
                onClick = { onSelect(code) },
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun EcritureFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) FormulooPrimary else FormulooSurface
    val textColor = if (selected) Color.White else FormulooOnSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = textColor)
    }
}

// ── Écriture card ─────────────────────────────────────────────────────────────

@Composable
private fun EcritureCard(
    ecriture: EcritureDto,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val badgeColor = journalCodeBadgeColor(ecriture.journalCode)
    val initiales = ecriture.journalCode.take(2).uppercase()
    val montant = ecriture.totalDebit.toDoubleOrNull()?.let {
        if (it > 0) formatAmount(it) else formatAmount(ecriture.totalCredit.toDoubleOrNull() ?: 0.0)
    } ?: "—"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FormulooSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Badge initiales
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initiales,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            // Infos principales
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ecriture.referencePiece.ifBlank { "#${ecriture.id}" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FormulooPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = ecriture.libelle.ifBlank { "Sans libellé" },
                    fontSize = 13.sp,
                    color = FormulooTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = formatDate(ecriture.dateEcriture),
                        fontSize = 11.sp,
                        color = FormulooOnSurfaceVariant,
                    )
                    StatutBadge(statut = ecriture.statut)
                }
            }
            Spacer(Modifier.width(8.dp))
            // Montant + chevron
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = montant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FormulooTextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = FormulooOnSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Lignes expandées
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                HorizontalDivider(color = FormulooOutline.copy(alpha = 0.5f), thickness = 0.5.dp)
                if (ecriture.lignes.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "Lignes non disponibles",
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                } else {
                    ecriture.lignes.forEachIndexed { idx, ligne ->
                        LigneRow(ligne = ligne)
                        if (idx < ecriture.lignes.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                color = FormulooOutline.copy(alpha = 0.3f),
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun LigneRow(ligne: LigneEcritureDto) {
    val debit = ligne.debit.toDoubleOrNull() ?: 0.0
    val credit = ligne.credit.toDoubleOrNull() ?: 0.0
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ligne.compteNumero,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooPrimary,
            )
            Text(
                text = ligne.compteLibelle,
                fontSize = 11.sp,
                color = FormulooOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (debit > 0) formatAmount(debit) else "—",
            fontSize = 12.sp,
            color = if (debit > 0) FormulooTextPrimary else FormulooOnSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = if (credit > 0) formatAmount(credit) else "—",
            fontSize = 12.sp,
            color = if (credit > 0) FormulooTextPrimary else FormulooOnSurfaceVariant,
            modifier = Modifier.width(80.dp),
        )
    }
}

// ── Status badge ──────────────────────────────────────────────────────────────

@Composable
private fun StatutBadge(statut: String) {
    val (bg, textColor, label) = when (statut.uppercase()) {
        "VALIDEE" -> Triple(
            FormulooPrimary.copy(alpha = 0.12f),
            FormulooPrimary,
            "Validée",
        )
        else -> Triple(
            FormulooSecondaryBg,
            FormulooSecondary,
            "Brouillon",
        )
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

// ── Date formatting ───────────────────────────────────────────────────────────

private fun formatDate(dateStr: String): String {
    val parts = dateStr.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else dateStr
}
