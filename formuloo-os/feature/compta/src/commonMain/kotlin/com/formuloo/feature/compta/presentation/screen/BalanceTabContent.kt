package com.formuloo.feature.compta.presentation.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.formuloo.core.network.dto.compta.BalanceLigneDto
import com.formuloo.feature.compta.presentation.viewmodel.BalanceSoldeFilter
import com.formuloo.feature.compta.presentation.viewmodel.BalanceUiState

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
internal fun BalanceDesComptesTab(
    state: BalanceUiState,
    onRetry: () -> Unit,
    onSelectExercice: (Int) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onSetClasseFilter: (Int?) -> Unit,
    onSetSoldeFilter: (BalanceSoldeFilter) -> Unit,
    onToggleShowZero: () -> Unit,
    onRefresh: () -> Unit,
) {
    when {
        state.isLoadingInit -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FormulooPrimary)
        }

        state.initError != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.initError, color = FormulooError, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) { Text("Réessayer", color = FormulooPrimary) }
            }
        }

        else -> BalanceContent(state = state, onRefresh = onRefresh)
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun BalanceContent(state: BalanceUiState, onRefresh: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FormulooBackground),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Title header (no stats block)
        item {
            BalanceTitleHeader(state)
        }

        // Balance body
        when {
            state.isLoadingBalance -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
            }

            state.balanceError != null -> item {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.balanceError, color = FormulooError, fontSize = 13.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onRefresh) { Text("Réessayer", color = FormulooPrimary) }
                }
            }

            state.balance != null -> {
                val lignes = state.filteredLignes

                if (lignes.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Aucun mouvement enregistré pour cet exercice.",
                                color = FormulooOnSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    // Flat list — no class headers
                    item {
                        Spacer(Modifier.height(4.dp))
                    }
                    items(lignes, key = { it.compteNumero }) { ligne ->
                        BalanceLigneSimpleRow(ligne)
                    }

                    // Totals row
                    item {
                        Spacer(Modifier.height(8.dp))
                        BalanceSimpleTotalsRow(state)
                    }

                    // Equilibre banner
                    item {
                        Spacer(Modifier.height(4.dp))
                        BalanceEquilibreBanner(isEquilibre = state.isEquilibre)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ── Title header ─────────────────────────────────────────────────────────────

@Composable
private fun BalanceTitleHeader(state: BalanceUiState) {
    val exercice = state.selectedExercice
    val subtitle = when {
        exercice == null -> "—"
        exercice.statut != "OUVERT" -> {
            val dateStr = exercice.dateCloture ?: exercice.dateFin
            "Exercice ${exercice.annee} · arrêtée au ${formatDate(dateStr)}"
        }
        else -> "Exercice ${exercice.annee} · en cours"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            "Balance générale",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = FormulooTextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
    }
}

// ── Flat ligne row ─────────────────────────────────────────────────────────────

@Composable
private fun BalanceLigneSimpleRow(ligne: BalanceLigneDto) {
    val sdD = ligne.soldeDebiteur.toDoubleOrNull() ?: 0.0
    val sdC = ligne.soldeCrediteur.toDoubleOrNull() ?: 0.0
    val isDebiteur = sdD > 0
    val amount = if (isDebiteur) sdD else sdC
    val indicator = if (isDebiteur) "D" else "C"
    val amountColor = if (isDebiteur) FormulooPrimary else FormulooSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Code
        Text(
            ligne.compteNumero,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = FormulooPrimary,
            modifier = Modifier.width(46.dp),
        )
        // Libellé
        Text(
            ligne.compteLibelle,
            fontSize = 13.sp,
            color = FormulooOnSurfaceVariant,
            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // Amount + D/C indicator
        if (amount > 0) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End) {
                Text(
                    formatAmount(amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
                Text(
                    indicator,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }
        } else {
            Text(
                "—",
                fontSize = 13.sp,
                color = FormulooOutline.copy(alpha = 0.5f),
            )
        }
    }
}

// ── Totals row ────────────────────────────────────────────────────────────────

@Composable
private fun BalanceSimpleTotalsRow(state: BalanceUiState) {
    val total = state.totalMouvementsDebit
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FormulooMint)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Totaux (débit = crédit)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = FormulooTextPrimary,
        )
        Text(
            "${formatCompact(total)} F",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = FormulooPrimary,
        )
    }
}

// ── Equilibre banner ──────────────────────────────────────────────────────────

@Composable
private fun BalanceEquilibreBanner(isEquilibre: Boolean) {
    val color = if (isEquilibre) FormulooPrimary else FormulooSecondary
    val bg = if (isEquilibre) FormulooMint else FormulooSecondaryBg
    val icon = if (isEquilibre) Icons.Filled.CheckCircle else Icons.Filled.Warning
    val text = if (isEquilibre)
        "Balance équilibrée — total débit = total crédit"
    else
        "Balance déséquilibrée — vérifier les écritures"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatCompact(value: Double): String = when {
    value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000)} G"
    value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)} M"
    value >= 1_000 -> "${"%.1f".format(value / 1_000)} K"
    else -> formatAmount(value)
}

private fun formatDate(dateStr: String): String {
    val parts = dateStr.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else dateStr
}
