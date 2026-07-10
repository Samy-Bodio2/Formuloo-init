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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.formuloo.core.network.dto.compta.BalanceDto
import com.formuloo.core.network.dto.compta.BilanDto
import com.formuloo.core.network.dto.compta.CompteResultatDto
import com.formuloo.core.network.dto.compta.ExerciceDto
import com.formuloo.core.network.dto.compta.GrandLivreCompteDto
import com.formuloo.feature.compta.presentation.viewmodel.EtatType
import com.formuloo.feature.compta.presentation.viewmodel.EtatsFinanciersUiState

@Composable
internal fun EtatsFinanciersTab(
    state: EtatsFinanciersUiState,
    onRetry: () -> Unit,
    onSelectEtat: (EtatType) -> Unit,
    onSelectExercice: (Int) -> Unit,
    onSetGLCompteNumero: (String) -> Unit,
    onRefreshBalance: () -> Unit,
    onRefreshBilan: () -> Unit,
    onRefreshResultat: () -> Unit,
) {
    when {
        state.isLoadingInit -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FormulooPrimary)
        }

        state.initError != null -> Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.initError, color = FormulooError, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) { Text("Réessayer", color = FormulooPrimary) }
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(FormulooBackground),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // ── Sélecteur d'exercice ───────────────────────────────────
                item {
                    ExerciceSelector(
                        exercices = state.exercices,
                        selectedId = state.selectedExerciceId,
                        onSelect = onSelectExercice,
                    )
                }

                // ── Sub-tabs ───────────────────────────────────────────────
                item {
                    EtatSubTabs(selected = state.selectedEtat, onSelect = onSelectEtat)
                }

                // ── Contenu selon l'état sélectionné ──────────────────────
                when (state.selectedEtat) {
                    EtatType.BALANCE -> item {
                        BalanceContent(
                            balance = state.balance,
                            isLoading = state.isLoadingBalance,
                            error = state.balanceError,
                            exercice = state.selectedExercice,
                            onRefresh = onRefreshBalance,
                        )
                    }

                    EtatType.BILAN -> item {
                        BilanContent(
                            bilan = state.bilan,
                            isLoading = state.isLoadingBilan,
                            error = state.bilanError,
                            exercice = state.selectedExercice,
                            onRefresh = onRefreshBilan,
                        )
                    }

                    EtatType.RESULTAT -> item {
                        ResultatContent(
                            resultat = state.compteResultat,
                            isLoading = state.isLoadingResultat,
                            error = state.resultatError,
                            exercice = state.selectedExercice,
                            onRefresh = onRefreshResultat,
                        )
                    }

                    EtatType.GRAND_LIVRE -> {
                        item {
                            GrandLivreSearchBar(
                                value = state.glCompteNumero,
                                onValueChange = onSetGLCompteNumero,
                                isLoading = state.isLoadingGL,
                                matchedLibelle = state.glCompte?.compteLibelle,
                                error = state.glError,
                            )
                        }
                        if (state.glCompte != null) {
                            item { GrandLivreHeader(state.glCompte) }
                            if (state.glCompte.lignes.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("Aucun mouvement.", color = FormulooOnSurfaceVariant, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                item { GrandLivreColumnHeaders() }
                                items(state.glCompte.lignes, key = { "${it.dateEcriture}_${it.debit}_${it.credit}" }) { ligne ->
                                    GrandLivreLigneRow(ligne)
                                    HorizontalDivider(color = FormulooOutline.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                item { GrandLivreTotals(state.glCompte) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Sélecteur d'exercice ──────────────────────────────────────────────────────

@Composable
private fun ExerciceSelector(exercices: List<ExerciceDto>, selectedId: Int?, onSelect: (Int) -> Unit) {
    if (exercices.isEmpty()) return
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        exercices.forEach { ex ->
            val selected = ex.id == selectedId
            val isOuvert = ex.statut == "OUVERT"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) FormulooPrimary else FormulooSurface)
                    .border(1.dp, if (selected) FormulooPrimary else FormulooOutline, RoundedCornerShape(8.dp))
                    .clickable { onSelect(ex.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        ex.annee.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (selected) Color.White else FormulooTextPrimary,
                    )
                    Text(
                        if (isOuvert) "Ouvert" else "Clôturé",
                        fontSize = 9.sp,
                        color = if (selected) Color.White.copy(0.8f)
                        else if (isOuvert) FormulooPrimary else FormulooOnSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Sub-tabs ──────────────────────────────────────────────────────────────────

private data class EtatTabDef(val type: EtatType, val label: String, val icon: ImageVector)

private val ETAT_TABS = listOf(
    EtatTabDef(EtatType.BALANCE, "Balance", Icons.Filled.List),
    EtatTabDef(EtatType.BILAN, "Bilan", Icons.Filled.AccountBalance),
    EtatTabDef(EtatType.RESULTAT, "Résultat", Icons.Filled.BarChart),
    EtatTabDef(EtatType.GRAND_LIVRE, "Grand Livre", Icons.Filled.Search),
)

@Composable
private fun EtatSubTabs(selected: EtatType, onSelect: (EtatType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ETAT_TABS.forEach { tab ->
            val isSelected = selected == tab.type
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) FormulooPrimary else Color.Transparent)
                    .border(1.dp, if (isSelected) FormulooPrimary else FormulooOutline, RoundedCornerShape(20.dp))
                    .clickable { onSelect(tab.type) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    tab.icon, null,
                    tint = if (isSelected) Color.White else FormulooOnSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    tab.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else FormulooOnSurfaceVariant,
                )
            }
        }
    }
}

// ── Helpers partagés ──────────────────────────────────────────────────────────

@Composable
private fun EtatLoadingBox() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FormulooPrimary)
    }
}

@Composable
private fun EtatErrorBox(error: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(error, color = FormulooError, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onRetry) { Text("Réessayer", color = FormulooPrimary) }
    }
}

@Composable
private fun SectionCard(title: String, devise: String = "XAF", content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooTextPrimary)
                Text(devise, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, isTotal: Boolean = false, color: Color = FormulooTextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = if (isTotal) 14.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) color else FormulooOnSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${formatAmount(value.toDoubleOrNull() ?: 0.0)} XAF",
            fontSize = if (isTotal) 14.sp else 13.sp,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
    }
}

// ── Balance ───────────────────────────────────────────────────────────────────

@Composable
private fun BalanceContent(
    balance: BalanceDto?,
    isLoading: Boolean,
    error: String?,
    exercice: ExerciceDto?,
    onRefresh: () -> Unit,
) {
    Column {
        if (isLoading) { EtatLoadingBox(); return@Column }
        if (error != null) { EtatErrorBox(error, onRefresh); return@Column }
        if (balance == null) { EtatLoadingBox(); return@Column }

        // Totaux globaux
        SectionCard(title = "Totaux de la balance", devise = balance.devise) {
            AmountRow("Total mouvements débit", balance.totalDebit, isTotal = true, color = FormulooPrimary)
            AmountRow("Total mouvements crédit", balance.totalCredit, isTotal = true, color = FormulooSecondary)
            Spacer(Modifier.height(4.dp))
            val diff = (balance.totalDebit.toDoubleOrNull() ?: 0.0) - (balance.totalCredit.toDoubleOrNull() ?: 0.0)
            val isBalanced = kotlin.math.abs(diff) < 0.01
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isBalanced) FormulooMint else FormulooSecondaryBg)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (isBalanced) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    null,
                    tint = if (isBalanced) FormulooPrimary else FormulooSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isBalanced) "Balance équilibrée" else "Balance déséquilibrée (écart : ${formatAmount(kotlin.math.abs(diff))} XAF)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isBalanced) FormulooPrimary else FormulooSecondary,
                )
            }
        }

        // Lignes par classe
        val byClasse = balance.lignes.groupBy { it.classe }.toSortedMap()
        byClasse.forEach { (classe, lignes) ->
            SectionCard(title = "Classe $classe") {
                lignes.forEach { ligne ->
                    BalanceLigneRow(ligne)
                }
                HorizontalDivider(color = FormulooOutline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                val sousTotal = lignes.sumOf { it.totalDebit.toDoubleOrNull() ?: 0.0 }
                AmountRow(
                    "Sous-total classe $classe",
                    sousTotal.toString(),
                    isTotal = true,
                    color = FormulooPrimary,
                )
            }
        }

        // Refresh
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Actualiser", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BalanceLigneRow(ligne: com.formuloo.core.network.dto.compta.BalanceLigneDto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${ligne.compteNumero} — ${ligne.compteLibelle}",
                fontSize = 12.sp,
                color = FormulooTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            val sd = ligne.soldeDebiteur.toDoubleOrNull() ?: 0.0
            val sc = ligne.soldeCrediteur.toDoubleOrNull() ?: 0.0
            if (sd > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(formatAmount(sd), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FormulooPrimary)
                    SoldeTag("D", FormulooPrimary, FormulooMint)
                }
            } else if (sc > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(formatAmount(sc), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FormulooSecondary)
                    SoldeTag("C", FormulooSecondary, FormulooSecondaryBg)
                }
            }
        }
    }
    HorizontalDivider(color = FormulooOutline.copy(alpha = 0.2f))
}

@Composable
private fun SoldeTag(label: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(bgColor).padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

// ── Bilan ─────────────────────────────────────────────────────────────────────

@Composable
private fun BilanContent(
    bilan: BilanDto?,
    isLoading: Boolean,
    error: String?,
    exercice: ExerciceDto?,
    onRefresh: () -> Unit,
) {
    var showActif by remember { mutableStateOf(true) }

    if (isLoading) { EtatLoadingBox(); return }
    if (error != null) { EtatErrorBox(error, onRefresh); return }
    if (bilan == null) { EtatLoadingBox(); return }

    val totalActif = bilan.actif.totalActif.toDoubleOrNull() ?: 0.0
    val totalPassif = bilan.passif.totalPassif.toDoubleOrNull() ?: 0.0
    val totalBilan = if (bilan.equilibre) totalActif else maxOf(totalActif, totalPassif)

    Column {
        // ── Hero card ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FormulooPrimary)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AccountBalance, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Bilan comptable · SYSCOHADA", fontSize = 11.sp, color = Color.White.copy(0.75f))
                    Text(
                        formatCompact(totalBilan) + " F",
                        fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White,
                    )
                    Text(
                        "Total du bilan · Exercice ${exercice?.annee ?: "—"}",
                        fontSize = 12.sp, color = Color.White.copy(0.8f),
                    )
                }
            }
        }

        // ── Actif / Passif toggle ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(FormulooBackground)
                .padding(4.dp),
        ) {
            listOf(
                Triple("Actif", formatCompact(totalActif) + " F", true),
                Triple("Passif", formatCompact(totalPassif) + " F", false),
            ).forEach { (label, amount, isActif) ->
                val active = showActif == isActif
                val contentColor = if (active) if (isActif) FormulooPrimary else FormulooSecondary else FormulooOnSurfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) FormulooSurface else Color.Transparent)
                        .clickable { showActif = isActif }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            label,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp, color = contentColor,
                        )
                        Text(amount, fontSize = 11.sp, color = contentColor)
                    }
                }
            }
        }

        // ── Content card ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            if (showActif) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BilanLigne("Immobilisations", bilan.actif.immobilisations, Color(0xFF2196F3))
                    BilanLigne("Actif circulant", bilan.actif.actifCirculant, Color(0xFF00BCD4))
                    BilanLigne("Trésorerie active", bilan.actif.tresorerieActif, Color(0xFF4CAF50))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FormulooMint.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Total actif", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooPrimary)
                    Text(formatCompact(totalActif) + " F", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooPrimary)
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    BilanLigne("Capitaux propres", bilan.passif.capitauxPropres, Color(0xFF9C27B0))
                    BilanLigne("Dettes", bilan.passif.dettes, Color(0xFFFF5722))
                    BilanLigne("Trésorerie passive", bilan.passif.tresoreriePassif, Color(0xFF2196F3))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FormulooSecondaryBg)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Total passif", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooSecondary)
                    Text(formatCompact(totalPassif) + " F", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooSecondary)
                }
            }
        }

        // ── Équilibre banner ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (bilan.equilibre) FormulooMint else FormulooSecondaryBg)
                .border(
                    1.dp,
                    if (bilan.equilibre) FormulooPrimary.copy(0.2f) else FormulooSecondary.copy(0.2f),
                    RoundedCornerShape(10.dp),
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (bilan.equilibre) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                null,
                tint = if (bilan.equilibre) FormulooPrimary else FormulooSecondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                if (bilan.equilibre) "Actif = Passif — bilan équilibré" else "Bilan déséquilibré",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (bilan.equilibre) FormulooPrimary else FormulooSecondary,
            )
        }

        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Actualiser", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BilanLigne(label: String, value: String, dotColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 13.sp, color = FormulooTextPrimary)
        }
        Text(
            formatCompact(value.toDoubleOrNull() ?: 0.0) + " F",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary,
        )
    }
}

// ── Compte de résultat ────────────────────────────────────────────────────────

@Composable
private fun ResultatContent(
    resultat: CompteResultatDto?,
    isLoading: Boolean,
    error: String?,
    exercice: ExerciceDto?,
    onRefresh: () -> Unit,
) {
    if (isLoading) { EtatLoadingBox(); return }
    if (error != null) { EtatErrorBox(error, onRefresh); return }
    if (resultat == null) { EtatLoadingBox(); return }

    val resultatNet = resultat.resultatNet.toDoubleOrNull() ?: 0.0
    val totalProduits = resultat.produits.totalProduits.toDoubleOrNull() ?: 0.0
    val totalCharges = resultat.charges.totalCharges.toDoubleOrNull() ?: 0.0
    val ca = (resultat.produits.chiffreAffaires.toDoubleOrNull() ?: 0.0).let { if (it > 0) it else 1.0 }
    val isBenefice = resultatNet >= 0
    val marge = (resultatNet / ca * 100).toInt()

    Column {
        // ── Hero card ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(FormulooPrimary)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Résultat net · Exercice ${exercice?.annee ?: "—"}",
                        fontSize = 11.sp, color = Color.White.copy(0.75f),
                    )
                    Text(
                        formatCompact(kotlin.math.abs(resultatNet)) + " F",
                        fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White,
                    )
                    Text(
                        if (isBenefice) "Bénéfice · marge $marge % du CA" else "Déficit de l'exercice",
                        fontSize = 12.sp, color = Color.White.copy(0.8f),
                    )
                }
            }
        }

        // ── Produits section ───────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "PRODUITS · CLASSE 7",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = FormulooOnSurfaceVariant, letterSpacing = 0.5.sp,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = FormulooOutline.copy(0.4f))
                ResultatLigne("Chiffre d'affaires", resultat.produits.chiffreAffaires, FormulooPrimary)
                val autresProduits = resultat.produits.autresProduits.toDoubleOrNull() ?: 0.0
                if (autresProduits > 0) {
                    ResultatLigne("Autres produits", resultat.produits.autresProduits, FormulooPrimary)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = FormulooOutline.copy(0.4f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total produits", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooPrimary)
                    Text(formatCompact(totalProduits) + " F", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooPrimary)
                }
            }
        }

        // ── Charges section ────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "CHARGES · CLASSE 6",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = FormulooOnSurfaceVariant, letterSpacing = 0.5.sp,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = FormulooOutline.copy(0.4f))
                ChargeLigne("Charges d'exploitation", resultat.charges.chargesExploitation, Color(0xFFFF6B35))
                val autresCharges = resultat.charges.autresCharges.toDoubleOrNull() ?: 0.0
                if (autresCharges > 0) {
                    ChargeLigne("Autres charges", resultat.charges.autresCharges, Color(0xFFE91E63))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(FormulooSecondaryBg).padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total charges", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooSecondary)
                Text(formatCompact(totalCharges) + " F", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooSecondary)
            }
        }

        // ── Résultat net card ──────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBenefice) FormulooMint else FormulooSecondaryBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents, null,
                        tint = if (isBenefice) FormulooPrimary else FormulooSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Résultat net", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooTextPrimary)
                    Text(
                        "Produits ${formatCompact(totalProduits)} F – Charges ${formatCompact(totalCharges)} F",
                        fontSize = 11.sp, color = FormulooOnSurfaceVariant,
                    )
                }
                Text(
                    formatCompact(kotlin.math.abs(resultatNet)) + " F",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = if (isBenefice) FormulooPrimary else FormulooError,
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Actualiser", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ResultatLigne(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = FormulooOnSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            formatCompact(value.toDoubleOrNull() ?: 0.0) + " F",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color,
        )
    }
}

@Composable
private fun ChargeLigne(label: String, value: String, dotColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
        }
        Text(
            formatCompact(value.toDoubleOrNull() ?: 0.0) + " F",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary,
        )
    }
}

private fun formatCompact(value: Double): String = when {
    value >= 1_000_000_000 -> "${(value / 1_000_000_000).toLong()} Md"
    value >= 1_000_000 -> "${(value / 1_000_000).toLong()} M"
    value >= 1_000 -> "${(value / 1_000).toLong()} k"
    else -> formatAmount(value)
}

// ── Grand Livre ───────────────────────────────────────────────────────────────

@Composable
private fun GrandLivreSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    matchedLibelle: String?,
    error: String?,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Numéro de compte",
            fontSize = 11.sp,
            color = FormulooOnSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(FormulooSurface)
                .border(1.dp, if (error != null) FormulooError else FormulooOutline, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search, null,
                tint = if (error != null) FormulooError else FormulooOnSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FormulooPrimary),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("Ex : 411, 521, 601…", fontSize = 14.sp, color = FormulooOnSurfaceVariant.copy(0.5f))
                    inner()
                },
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = FormulooPrimary)
            }
        }
        if (matchedLibelle != null) {
            Text(matchedLibelle, fontSize = 12.sp, color = FormulooPrimary, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
        }
        if (error != null) {
            Text(error, fontSize = 12.sp, color = FormulooError, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
        }
    }
}

@Composable
private fun GrandLivreHeader(gl: GrandLivreCompteDto) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooMint),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "${gl.compteNumero} — ${gl.compteLibelle}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = FormulooPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("Mvt Débit", fontSize = 10.sp, color = FormulooPrimary.copy(0.7f))
                    Text(formatAmount(gl.totalDebit.toDoubleOrNull() ?: 0.0), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary)
                }
                Column {
                    Text("Mvt Crédit", fontSize = 10.sp, color = FormulooSecondary.copy(0.7f))
                    Text(formatAmount(gl.totalCredit.toDoubleOrNull() ?: 0.0), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FormulooSecondary)
                }
                val solde = gl.soldeFinal.toDoubleOrNull() ?: 0.0
                Column {
                    Text("Solde final", fontSize = 10.sp, color = FormulooOnSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            formatAmount(kotlin.math.abs(solde)),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (solde >= 0) FormulooPrimary else FormulooSecondary,
                        )
                        SoldeTag(if (solde >= 0) "D" else "C", if (solde >= 0) FormulooPrimary else FormulooSecondary, Color.White.copy(0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GrandLivreColumnHeaders() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooOutline.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Date", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FormulooOnSurfaceVariant, modifier = Modifier.width(60.dp))
        Text("Libellé", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FormulooOnSurfaceVariant, modifier = Modifier.weight(1f))
        Text("Jnl", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FormulooOnSurfaceVariant, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
        Text("Débit", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
        Text("Crédit", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FormulooSecondary, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
        Text("Solde", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FormulooOnSurfaceVariant, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun GrandLivreLigneRow(ligne: com.formuloo.core.network.dto.compta.GrandLivreLigneDto) {
    val solde = ligne.soldeCumule.toDoubleOrNull() ?: 0.0
    val debit = ligne.debit.toDoubleOrNull() ?: 0.0
    val credit = ligne.credit.toDoubleOrNull() ?: 0.0
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ligne.dateEcriture.drop(2), fontSize = 10.sp, color = FormulooOnSurfaceVariant, modifier = Modifier.width(60.dp))
        Text(
            ligne.libelle,
            fontSize = 11.sp,
            color = FormulooTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            ligne.journalCode,
            fontSize = 9.sp,
            color = FormulooPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            if (debit > 0) formatAmount(debit) else "—",
            fontSize = 11.sp,
            color = if (debit > 0) FormulooPrimary else FormulooOnSurfaceVariant.copy(0.4f),
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
        )
        Text(
            if (credit > 0) formatAmount(credit) else "—",
            fontSize = 11.sp,
            color = if (credit > 0) FormulooSecondary else FormulooOnSurfaceVariant.copy(0.4f),
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
        )
        Text(
            formatAmount(kotlin.math.abs(solde)),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (solde >= 0) FormulooPrimary else FormulooSecondary,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun GrandLivreTotals(gl: GrandLivreCompteDto) {
    val solde = gl.soldeFinal.toDoubleOrNull() ?: 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooOutline.copy(0.1f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("TOTAUX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FormulooTextPrimary, modifier = Modifier.weight(1f).padding(start = 64.dp))
        Text("", modifier = Modifier.width(28.dp))
        Text(
            formatAmount(gl.totalDebit.toDoubleOrNull() ?: 0.0),
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary,
            modifier = Modifier.width(72.dp), textAlign = TextAlign.End,
        )
        Text(
            formatAmount(gl.totalCredit.toDoubleOrNull() ?: 0.0),
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FormulooSecondary,
            modifier = Modifier.width(72.dp), textAlign = TextAlign.End,
        )
        Text(
            formatAmount(kotlin.math.abs(solde)),
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = if (solde >= 0) FormulooPrimary else FormulooSecondary,
            modifier = Modifier.width(72.dp), textAlign = TextAlign.End,
        )
    }
}
