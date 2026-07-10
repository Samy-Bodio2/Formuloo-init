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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
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
import com.formuloo.core.network.dto.compta.DeclarationTVADto
import com.formuloo.feature.compta.presentation.viewmodel.DeclarationTVAUiState
import com.formuloo.feature.compta.presentation.viewmodel.MOIS_LABELS
import com.formuloo.feature.compta.presentation.viewmodel.TVAPeriodeMode

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
internal fun DeclarationTVATab(
    state: DeclarationTVAUiState,
    onRetry: () -> Unit,
    onSetPeriodeMode: (TVAPeriodeMode) -> Unit,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit,
    onSelectTrimestre: (Int) -> Unit,
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

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(FormulooBackground),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                // ── Sélecteur de période (chips scrollables) ───────────────
                item {
                    TVAPeriodeChipsRow(
                        selectedYear = state.selectedYear,
                        selectedMonth = state.selectedMonth,
                        availableYears = state.availableYears,
                        onSelect = { year, month ->
                            if (year != state.selectedYear) onSelectYear(year)
                            if (month != state.selectedMonth) onSelectMonth(month)
                        },
                    )
                }

                // ── Contenu de la déclaration ──────────────────────────────
                item {
                    when {
                        state.isLoading -> Box(
                            Modifier.fillMaxWidth().padding(56.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = FormulooPrimary) }

                        state.error != null -> Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(state.error, color = FormulooError, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(10.dp))
                            TextButton(onClick = onRefresh) {
                                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Réessayer", color = FormulooPrimary)
                            }
                        }

                        state.declaration != null -> TVADeclarationContent(
                            d = state.declaration,
                            periodeLabel = state.periodeLabel,
                            selectedYear = state.selectedYear,
                            selectedMonth = state.selectedMonth,
                        )
                    }
                }
            }
        }
    }
}

// ── Sélecteur de période — chips scrollables ──────────────────────────────────

@Composable
private fun TVAPeriodeChipsRow(
    selectedYear: Int,
    selectedMonth: Int,
    availableYears: List<Int>,
    onSelect: (year: Int, month: Int) -> Unit,
) {
    // Generate chips: all months across all years, most recent first
    data class PeriodChip(val year: Int, val month: Int, val label: String)

    val chips: List<PeriodChip> = buildList {
        val years = availableYears.sortedDescending().ifEmpty { listOf(selectedYear) }
        for (year in years) {
            for (month in 12 downTo 1) {
                add(PeriodChip(year, month, "${MOIS_LABELS[month - 1]} $year"))
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val isSelected = chip.year == selectedYear && chip.month == selectedMonth
            PeriodeChip(
                label = chip.label,
                selected = isSelected,
                onClick = { onSelect(chip.year, chip.month) },
            )
        }
    }
}

@Composable
private fun PeriodeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) FormulooPrimary else Color.Transparent
    val borderColor = if (selected) FormulooPrimary else FormulooOutline
    val textColor = if (selected) Color.White else FormulooOnSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp),
        )
    }
}

// ── Contenu de la déclaration ─────────────────────────────────────────────────

@Composable
private fun TVADeclarationContent(
    d: DeclarationTVADto,
    periodeLabel: String,
    selectedYear: Int,
    selectedMonth: Int,
) {
    val collectee = d.tvaCollectee.toDoubleOrNull() ?: 0.0
    val deductible = d.tvaDeductible.total.toDoubleOrNull() ?: 0.0
    val montantAPayer = d.montantAPayer.toDoubleOrNull() ?: 0.0
    val creditReporte = d.creditReporte.toDoubleOrNull() ?: 0.0
    val isAPayer = d.resultat == "TVA_A_PAYER"
    val mainAmount = if (isAPayer) montantAPayer else creditReporte

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // ── Bannière héro TVA ──────────────────────────────────────────────
        TVAHeroBanner(
            isAPayer = isAPayer,
            amount = mainAmount,
            devise = d.devise,
            periodeLabel = periodeLabel,
        )

        // ── Tableau de calcul ──────────────────────────────────────────────
        TVACalculTableCard(
            collectee = collectee,
            deductible = deductible,
            creditPrecedent = creditReporte.takeIf { !isAPayer && it > 0 } ?: 0.0,
            isAPayer = isAPayer,
            mainAmount = mainAmount,
            devise = d.devise,
        )

        // ── Infos complémentaires ──────────────────────────────────────────
        TVAInfoCard(
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
        )

        // ── Bannière légale ────────────────────────────────────────────────
        TVALegalBanner()

        Spacer(Modifier.height(8.dp))
    }
}

// ── Bannière héro ─────────────────────────────────────────────────────────────

@Composable
private fun TVAHeroBanner(
    isAPayer: Boolean,
    amount: Double,
    devise: String,
    periodeLabel: String,
) {
    val bgColor = if (isAPayer) FormulooPrimary else FormulooSecondary
    val badgeBg = Color.White.copy(alpha = 0.15f)
    val label = if (isAPayer) "TVA nette à payer" else "Crédit de TVA reportable"
    val sublabel = "$periodeLabel · régime réel · déclarée"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // % badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Column {
                Text(
                    label,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatAmount(amount)} $devise",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    sublabel,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ── Tableau de calcul ─────────────────────────────────────────────────────────

@Composable
private fun TVACalculTableCard(
    collectee: Double,
    deductible: Double,
    creditPrecedent: Double,
    isAPayer: Boolean,
    mainAmount: Double,
    devise: String,
) {
    val resultBg = if (isAPayer) FormulooMint else FormulooSecondaryBg
    val resultColor = if (isAPayer) FormulooPrimary else FormulooSecondary
    val resultLabel = if (isAPayer) "TVA due à la DGI" else "Crédit à reporter"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FormulooSurface),
    ) {
        // Lignes de comptes
        TVACompteLigne(
            code = "443",
            libelle = "TVA collectée sur ventes",
            amount = collectee,
            isNegative = false,
        )
        HorizontalDivider(color = FormulooOutline.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
        TVACompteLigne(
            code = "445",
            libelle = "TVA déductible sur achats",
            amount = deductible,
            isNegative = true,
        )
        if (creditPrecedent > 0) {
            HorizontalDivider(color = FormulooOutline.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
            TVACompteLigne(
                code = "449",
                libelle = "Crédit de TVA reporté",
                amount = creditPrecedent,
                isNegative = true,
            )
        }

        // Ligne de résultat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(resultBg)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                resultLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = FormulooTextPrimary,
            )
            Text(
                "${formatAmount(mainAmount)} $devise",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = resultColor,
            )
        }
    }
}

@Composable
private fun TVACompteLigne(code: String, libelle: String, amount: Double, isNegative: Boolean) {
    val amountColor = if (isNegative) FormulooSecondary else FormulooTextPrimary
    val amountPrefix = if (isNegative) "– " else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                code,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooPrimary,
                modifier = Modifier.width(32.dp),
            )
            Text(
                libelle,
                fontSize = 13.sp,
                color = FormulooOnSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            "$amountPrefix${formatAmount(amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
    }
}

// ── Carte infos complémentaires ───────────────────────────────────────────────

@Composable
private fun TVAInfoCard(selectedYear: Int, selectedMonth: Int) {
    val echeance = run {
        val nextMonth = if (selectedMonth == 12) 1 else selectedMonth + 1
        val nextYear = if (selectedMonth == 12) selectedYear + 1 else selectedYear
        "15 ${MOIS_LABELS[nextMonth - 1]} $nextYear"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FormulooSurface),
    ) {
        TVAInfoLigne(
            icon = Icons.Filled.CalendarToday,
            label = "Échéance de télédéclaration",
            value = echeance,
        )
        HorizontalDivider(color = FormulooOutline.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
        TVAInfoLigne(
            icon = Icons.Filled.Percent,
            label = "Taux applicable",
            value = "19,25 %",
        )
    }
}

@Composable
private fun TVAInfoLigne(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = FormulooPrimary, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
        }
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary)
    }
}

// ── Bannière légale ───────────────────────────────────────────────────────────

@Composable
private fun TVALegalBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FormulooMint)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = FormulooPrimary,
            modifier = Modifier.size(16.dp).padding(top = 1.dp),
        )
        Text(
            "Formulaire DTI conforme à la DGI Cameroun. Le montant est calculé automatiquement à partir des comptes 443 (collectée) et 445 (déductible) de la période.",
            fontSize = 12.sp,
            color = FormulooOnSurfaceVariant,
            lineHeight = 17.sp,
        )
    }
}
