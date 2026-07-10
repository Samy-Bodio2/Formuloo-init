package com.formuloo.feature.compta.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.formuloo.core.network.dto.compta.ImmobilisationDto
import com.formuloo.core.network.dto.compta.PlanAmortissementDto
import com.formuloo.feature.compta.presentation.viewmodel.ImmobilisationsUiState
import com.formuloo.feature.compta.presentation.viewmodel.ImmobilisationsViewModel
import com.formuloo.feature.compta.presentation.viewmodel.COMPTE_PAR_CATEGORIE

private val CATEGORIE_LABELS = mapOf(
    "INCORPORELLE" to "Incorporelle",
    "TERRAIN" to "Terrain",
    "CONSTRUCTION" to "Construction",
    "MATERIEL" to "Matériel",
    "MOBILIER" to "Mobilier",
    "VEHICULE" to "Véhicule",
    "FINANCIERE" to "Financière",
)

private val METHODE_LABELS = mapOf(
    "LINEAIRE" to "Linéaire",
    "DEGRESSIF" to "Dégressif",
    "NON_AMORTISSABLE" to "Non amortissable",
)

private val STATUT_COLORS = mapOf(
    "ACTIVE" to Color(0xFF2E7D32),
    "AMORTIE" to Color(0xFF1565C0),
    "CEDEE" to Color(0xFF6D4C41),
)

private val STATUT_BG = mapOf(
    "ACTIVE" to Color(0xFFE8F5E9),
    "AMORTIE" to Color(0xFFE3F2FD),
    "CEDEE" to Color(0xFFEFEBE9),
)

private val STATUT_LABELS = mapOf(
    "ACTIVE" to "Active",
    "AMORTIE" to "Amortie",
    "CEDEE" to "Cédée",
)

@Composable
fun ImmobilisationsTab(
    viewModel: ImmobilisationsViewModel,
    state: ImmobilisationsUiState,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showCreateDialog,
                containerColor = FormulooPrimary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter immobilisation")
            }
        },
        containerColor = FormulooBackground,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                state.error != null -> ImmoErrorState(state.error, viewModel::load)
                else -> ImmoMainContent(state, viewModel)
            }
        }
    }

    // Dialogs overlay
    if (state.showCreateDialog) {
        CreateImmobilisationDialog(state, viewModel)
    }
    if (state.showAmortirDialog) {
        AmortirConfirmDialog(state, viewModel)
    }
    if (state.amortirResult != null) {
        AmortirResultDialog(state, viewModel)
    }
    if (state.showCederDialog) {
        CederConfirmDialog(state, viewModel)
    }
}

@Composable
private fun ImmoErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Business, contentDescription = null, tint = FormulooOnSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(error, color = FormulooError, textAlign = TextAlign.Center, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Réessayer", color = FormulooPrimary)
        }
    }
}

@Composable
private fun ImmoMainContent(state: ImmobilisationsUiState, viewModel: ImmobilisationsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        item { ImmoStatsHeader(state) }
        item { ImmoStatutFilterPills(state, viewModel) }

        if (state.filteredImmobilisations.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune immobilisation trouvée", color = FormulooOnSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(state.filteredImmobilisations, key = { it.id }) { immo ->
                ImmobilisationCard(
                    immo = immo,
                    isExpanded = state.expandedImmoId == immo.id,
                    plan = state.plans[immo.id],
                    isLoadingPlan = state.loadingPlanForId == immo.id,
                    planError = if (state.expandedImmoId == immo.id) state.planError else null,
                    onToggleExpand = { viewModel.toggleExpand(immo.id) },
                    onAmortir = { viewModel.requestAmortir(immo) },
                    onCeder = { viewModel.requestCeder(immo) },
                )
            }
        }
    }
}

@Composable
private fun ImmoStatsHeader(state: ImmobilisationsUiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ImmoStatCard("Total", state.immobilisations.size.toString(), FormulooPrimary, Modifier.weight(1f))
            ImmoStatCard("Actives", state.nbActive.toString(), Color(0xFF2E7D32), Modifier.weight(1f))
            ImmoStatCard("Amorties", state.nbAmortie.toString(), Color(0xFF1565C0), Modifier.weight(1f))
            ImmoStatCard("Cédées", state.nbCedee.toString(), FormulooOnSurfaceVariant, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ImmoAmountCard("Valeur brute", formatAmount(state.valeurBrute), FormulooPrimary, Modifier.weight(1f))
            ImmoAmountCard("Valeur nette", formatAmount(state.valeurNetteTotal), Color(0xFF2E7D32), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ImmoStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = FormulooOnSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ImmoAmountCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 12.sp, color = FormulooOnSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun ImmoStatutFilterPills(state: ImmobilisationsUiState, viewModel: ImmobilisationsViewModel) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(null to "Toutes", "ACTIVE" to "Actives", "AMORTIE" to "Amorties", "CEDEE" to "Cédées").forEach { (key, label) ->
            val selected = state.statutFilter == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) FormulooPrimary else FormulooSurface)
                    .border(1.dp, if (selected) FormulooPrimary else FormulooOutline, RoundedCornerShape(20.dp))
                    .clickable { viewModel.setStatutFilter(key) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(label, fontSize = 13.sp, color = if (selected) Color.White else FormulooTextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ImmobilisationCard(
    immo: ImmobilisationDto,
    isExpanded: Boolean,
    plan: PlanAmortissementDto?,
    isLoadingPlan: Boolean,
    planError: String?,
    onToggleExpand: () -> Unit,
    onAmortir: () -> Unit,
    onCeder: () -> Unit,
) {
    val valeurOrigine = immo.valeurOrigine.toDoubleOrNull() ?: 1.0
    val cumul = immo.cumulAmortissements.toDoubleOrNull() ?: 0.0
    val vnc = immo.valeurNetteComptable.toDoubleOrNull() ?: 0.0
    val progress = if (valeurOrigine > 0) (cumul / valeurOrigine).toFloat().coerceIn(0f, 1f) else 0f
    val statutColor = STATUT_COLORS[immo.statut] ?: FormulooOnSurfaceVariant
    val statutBg = STATUT_BG[immo.statut] ?: FormulooSecondaryBg

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpand),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(immo.code, fontSize = 12.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium)
                        CategorieChip(immo.categorie)
                        ImmoStatutBadge(immo.statut, statutColor, statutBg)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(immo.designation, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${METHODE_LABELS[immo.methode] ?: immo.methode} · ${if (immo.dureeVie > 0) "${immo.dureeVie} ans" else "—"} · Cpte ${immo.numeroCompte}",
                        fontSize = 11.sp,
                        color = FormulooOnSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatAmount(vnc), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary)
                    Text("VNC", fontSize = 10.sp, color = FormulooOnSurfaceVariant)
                }
                Icon(
                    if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = FormulooOnSurfaceVariant,
                )
            }

            // Progress bar
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatAmount(cumul), fontSize = 10.sp, color = FormulooOnSurfaceVariant)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = when (immo.statut) {
                        "ACTIVE" -> FormulooPrimary
                        "AMORTIE" -> Color(0xFF1565C0)
                        else -> FormulooOnSurfaceVariant
                    },
                    trackColor = FormulooOutline,
                )
                Text(formatAmount(valeurOrigine), fontSize = 10.sp, color = FormulooOnSurfaceVariant)
            }

            // Action buttons
            if (immo.statut == "ACTIVE") {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onAmortir,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1565C0)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("Amortir", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onCeder,
                        colors = ButtonDefaults.textButtonColors(contentColor = FormulooError),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("Céder", fontSize = 12.sp)
                    }
                }
            }

            // Expanded plan
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = FormulooOutline)
                    when {
                        isLoadingPlan -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = FormulooPrimary)
                        }
                        planError != null -> Text(planError, color = FormulooError, fontSize = 12.sp)
                        plan != null -> PlanAmortissementTable(plan)
                        else -> Text("Plan non disponible", color = FormulooOnSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorieChip(categorie: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(FormulooSecondaryBg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(CATEGORIE_LABELS[categorie] ?: categorie, fontSize = 10.sp, color = FormulooSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ImmoStatutBadge(statut: String, color: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(STATUT_LABELS[statut] ?: statut, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PlanAmortissementTable(plan: PlanAmortissementDto) {
    Column {
        Text("Plan d'amortissement", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary)
        Spacer(Modifier.height(8.dp))
        // Header
        Row(modifier = Modifier.fillMaxWidth().background(FormulooBackground).padding(vertical = 5.dp, horizontal = 4.dp)) {
            Text("Année", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary, modifier = Modifier.width(44.dp))
            Text("Annuité", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("Cumul", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("VNC fin", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text("Réel", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        HorizontalDivider(color = FormulooOutline)
        plan.plan.forEach { ligne ->
            val montantReel = ligne.montantReel
            val rowBg = when {
                ligne.passe && montantReel != null -> Color(0xFFE8F5E9)
                ligne.passe -> Color(0xFFFFF3E0)
                else -> FormulooSurface
            }
            val textColor = if (ligne.passe) FormulooTextPrimary else FormulooOnSurfaceVariant
            Row(
                modifier = Modifier.fillMaxWidth().background(rowBg).padding(vertical = 5.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ligne.annee.toString(), fontSize = 11.sp, color = textColor, modifier = Modifier.width(44.dp))
                Text(formatAmount(ligne.annuitePrevue.toDoubleOrNull() ?: 0.0), fontSize = 11.sp, color = textColor, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text(formatAmount(ligne.cumulPrevu.toDoubleOrNull() ?: 0.0), fontSize = 11.sp, color = textColor, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text(formatAmount(ligne.vncFin.toDoubleOrNull() ?: 0.0), fontSize = 11.sp, color = textColor, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text(
                    if (montantReel != null) formatAmount(montantReel.toDoubleOrNull() ?: 0.0) else "—",
                    fontSize = 11.sp,
                    color = if (montantReel != null) Color(0xFF2E7D32) else FormulooOnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
            HorizontalDivider(color = FormulooOutline.copy(alpha = 0.5f))
        }
    }
}

// ── Create Dialog ─────────────────────────────────────────────────────────────

@Composable
private fun CreateImmobilisationDialog(state: ImmobilisationsUiState, viewModel: ImmobilisationsViewModel) {
    val categories = listOf("INCORPORELLE", "TERRAIN", "CONSTRUCTION", "MATERIEL", "MOBILIER", "VEHICULE", "FINANCIERE")
    val methodes = listOf("LINEAIRE", "DEGRESSIF", "NON_AMORTISSABLE")

    AlertDialog(
        onDismissRequest = viewModel::dismissCreateDialog,
        containerColor = FormulooSurface,
        title = {
            Text("Nouvelle immobilisation", fontWeight = FontWeight.Bold, color = FormulooTextPrimary, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CreateFieldRow("Code", state.createCode, viewModel::setCreateCode, "IMM-001")
                CreateFieldRow("Désignation *", state.createDesignation, viewModel::setCreateDesignation, "Ex: Ordinateur portable")

                // Catégorie
                Column {
                    Text("Catégorie", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        categories.forEach { cat ->
                            val sel = state.createCategorie == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (sel) FormulooPrimary else FormulooBackground)
                                    .border(1.dp, if (sel) FormulooPrimary else FormulooOutline, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.setCreateCategorie(cat) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text(CATEGORIE_LABELS[cat] ?: cat, fontSize = 11.sp, color = if (sel) Color.White else FormulooTextPrimary)
                            }
                        }
                    }
                }

                CreateFieldRow("N° compte", state.createNumeroCompte, viewModel::setCreateNumeroCompte, "2184")
                CreateFieldRow("Valeur d'origine *", state.createValeurOrigine, viewModel::setCreateValeurOrigine, "500000", KeyboardType.Decimal)
                CreateFieldRow("Valeur résiduelle", state.createValeurResiduelle, viewModel::setCreateValeurResiduelle, "0", KeyboardType.Decimal)

                // Méthode
                if (state.createMethode != "NON_AMORTISSABLE") {
                    Column {
                        Text("Méthode", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            methodes.filter { it != "NON_AMORTISSABLE" }.forEach { m ->
                                val sel = state.createMethode == m
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (sel) FormulooPrimary else FormulooBackground)
                                        .border(1.dp, if (sel) FormulooPrimary else FormulooOutline, RoundedCornerShape(16.dp))
                                        .clickable { viewModel.setCreateMethode(m) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                ) {
                                    Text(METHODE_LABELS[m] ?: m, fontSize = 11.sp, color = if (sel) Color.White else FormulooTextPrimary)
                                }
                            }
                        }
                    }
                    CreateFieldRow("Durée de vie (ans)", state.createDureeVie, viewModel::setCreateDureeVie, "5", KeyboardType.Number)
                }

                CreateFieldRow("Date mise en service *", state.createDateMiseEnService, viewModel::setCreateDateMiseEnService, "AAAA-MM-JJ")
                CreateFieldRow("Fournisseur", state.createFournisseur, viewModel::setCreateFournisseur, "Nom du fournisseur")

                state.createError?.let {
                    Text(it, color = FormulooError, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::createImmobilisation,
                enabled = !state.isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
            ) {
                if (state.isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Créer", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissCreateDialog) {
                Text("Annuler", color = FormulooOnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun CreateFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column {
        Text(label, fontSize = 12.sp, color = FormulooOnSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FormulooOutline, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontSize = 13.sp, color = FormulooTextPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, fontSize = 13.sp, color = FormulooOutline)
                    inner()
                },
            )
        }
    }
}

// ── Amortir Dialog ────────────────────────────────────────────────────────────

@Composable
private fun AmortirConfirmDialog(state: ImmobilisationsUiState, viewModel: ImmobilisationsViewModel) {
    val immo = state.immoToAmortir ?: return
    AlertDialog(
        onDismissRequest = viewModel::dismissAmortirDialog,
        containerColor = FormulooSurface,
        title = {
            Text("Comptabiliser l'amortissement", fontWeight = FontWeight.Bold, color = FormulooTextPrimary, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amortir ${immo.designation} (${immo.code}) pour l'exercice en cours ?", fontSize = 13.sp, color = FormulooTextPrimary)
                Text("VNC actuelle : ${formatAmount(immo.valeurNetteComptable.toDoubleOrNull() ?: 0.0)}", fontSize = 13.sp, color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
                Text("Cette opération créera une écriture comptable de dotation aux amortissements (compte 6811).", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                state.amortirError?.let { Text(it, color = FormulooError, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::confirmAmortir,
                enabled = !state.isAmortissing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
            ) {
                if (state.isAmortissing) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Confirmer", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissAmortirDialog) {
                Text("Annuler", color = FormulooOnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun AmortirResultDialog(state: ImmobilisationsUiState, viewModel: ImmobilisationsViewModel) {
    val result = state.amortirResult ?: return
    AlertDialog(
        onDismissRequest = viewModel::dismissAmortirResult,
        containerColor = FormulooSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                Text("Amortissement comptabilisé", fontWeight = FontWeight.Bold, color = FormulooTextPrimary, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${result.dotation.immobilisationDesignation} (${result.dotation.immobilisationCode})", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                ResultInfoRow("Exercice", result.dotation.annee.toString())
                ResultInfoRow("Montant dotation", formatAmount(result.dotation.montant.toDoubleOrNull() ?: 0.0))
                ResultInfoRow("Nouvelle VNC", formatAmount(result.valeurNetteComptable.toDoubleOrNull() ?: 0.0))
                result.ecritureId?.let { ResultInfoRow("Écriture créée", "#$it") }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::dismissAmortirResult) {
                Text("Fermer", color = FormulooPrimary)
            }
        },
    )
}

@Composable
private fun ResultInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FormulooTextPrimary)
    }
}

// ── Céder Dialog ──────────────────────────────────────────────────────────────

@Composable
private fun CederConfirmDialog(state: ImmobilisationsUiState, viewModel: ImmobilisationsViewModel) {
    val immo = state.immoToCeder ?: return
    AlertDialog(
        onDismissRequest = viewModel::dismissCederDialog,
        containerColor = FormulooSurface,
        title = {
            Text("Céder l'immobilisation", fontWeight = FontWeight.Bold, color = FormulooError, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Céder ${immo.designation} (${immo.code}) ?", fontSize = 13.sp, color = FormulooTextPrimary)
                Text("VNC au moment de la cession : ${formatAmount(immo.valeurNetteComptable.toDoubleOrNull() ?: 0.0)}", fontSize = 12.sp, color = FormulooOnSurfaceVariant)

                Column {
                    Text("Valeur nette de cession (XAF)", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, FormulooOutline, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        BasicTextField(
                            value = state.cederValeurNette,
                            onValueChange = viewModel::setCederValeurNette,
                            textStyle = TextStyle(fontSize = 14.sp, color = FormulooTextPrimary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Text("Cette opération est irréversible. Le statut de l'immobilisation passera à CÉDÉE.", fontSize = 12.sp, color = FormulooError)
                state.cederError?.let { Text(it, color = FormulooError, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::confirmCeder,
                enabled = !state.isCeding,
                colors = ButtonDefaults.buttonColors(containerColor = FormulooError),
            ) {
                if (state.isCeding) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Confirmer la cession", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissCederDialog) {
                Text("Annuler", color = FormulooOnSurfaceVariant)
            }
        },
    )
}
