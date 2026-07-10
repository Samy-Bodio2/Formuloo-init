package com.formuloo.feature.compta.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.compta.domain.model.ClasseGroup
import com.formuloo.feature.compta.domain.model.DashboardStats
import com.formuloo.feature.compta.domain.model.PlanComptableStats
import com.formuloo.feature.compta.domain.model.SousGroupeGroup
import com.formuloo.feature.compta.presentation.viewmodel.ComptaDashboardViewModel
import com.formuloo.feature.compta.presentation.viewmodel.ComptaPlanViewModel
import com.formuloo.feature.compta.presentation.viewmodel.BalanceSoldeFilter
import com.formuloo.feature.compta.presentation.viewmodel.BalanceViewModel
import com.formuloo.feature.compta.presentation.viewmodel.DeclarationTVAViewModel
import com.formuloo.feature.compta.presentation.viewmodel.AvoirsViewModel
import com.formuloo.feature.compta.presentation.viewmodel.ImmobilisationsViewModel
import com.formuloo.feature.compta.presentation.viewmodel.TVAPeriodeMode
import com.formuloo.feature.compta.presentation.viewmodel.EtatType
import com.formuloo.feature.compta.presentation.viewmodel.EtatsFinanciersViewModel
import com.formuloo.feature.compta.presentation.viewmodel.ExercicesViewModel
import com.formuloo.feature.compta.presentation.viewmodel.EcrituresListViewModel
import com.formuloo.feature.compta.presentation.viewmodel.JournauxViewModel
import com.formuloo.feature.compta.presentation.viewmodel.PlanComptableUiState
import com.formuloo.feature.compta.presentation.viewmodel.SaisieViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComptaDashboardScreen(
    onBack: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToPurchaseInvoices: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToSupplierPayments: () -> Unit,
    dashboardViewModel: ComptaDashboardViewModel = koinViewModel(),
    planViewModel: ComptaPlanViewModel = koinViewModel(),
    saisieViewModel: SaisieViewModel = koinViewModel(),
    journauxViewModel: JournauxViewModel = koinViewModel(),
    exercicesViewModel: ExercicesViewModel = koinViewModel(),
    etatsViewModel: EtatsFinanciersViewModel = koinViewModel(),
    balanceViewModel: BalanceViewModel = koinViewModel(),
    tvaViewModel: DeclarationTVAViewModel = koinViewModel(),
    immoViewModel: ImmobilisationsViewModel = koinViewModel(),
    avoirsViewModel: AvoirsViewModel = koinViewModel(),
    ecrituresViewModel: EcrituresListViewModel = koinViewModel(),
) {
    val dashboardState by dashboardViewModel.statsState.collectAsStateWithLifecycle()
    val planState by planViewModel.state.collectAsStateWithLifecycle()
    val saisieState by saisieViewModel.state.collectAsStateWithLifecycle()
    val journauxState by journauxViewModel.state.collectAsStateWithLifecycle()
    val exercicesState by exercicesViewModel.state.collectAsStateWithLifecycle()
    val etatsState by etatsViewModel.state.collectAsStateWithLifecycle()
    val balanceState by balanceViewModel.state.collectAsStateWithLifecycle()
    val tvaState by tvaViewModel.state.collectAsStateWithLifecycle()
    val immoState by immoViewModel.state.collectAsStateWithLifecycle()
    val ecrituresState by ecrituresViewModel.state.collectAsStateWithLifecycle()

    val selectedTab = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Comptabilité",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = FormulooTextPrimary,
                        )
                        val annee = planState.stats?.exerciceAnnee
                        Text(
                            if (annee != null) "SYSCOHADA · Exercice $annee" else "SYSCOHADA · Plan de comptes",
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = FormulooTextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Exporter",
                            tint = FormulooTextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FormulooSurface,
                    titleContentColor = FormulooTextPrimary,
                    navigationIconContentColor = FormulooTextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // ── Custom tab bar ────────────────────────────────────────────────
            ComptaTabBar(
                selectedTab = selectedTab.value,
                onTabSelected = { selectedTab.value = it },
                nbEcrituresEnAttente = ecrituresState.nbEnAttente,
            )

            // ── Tab content ───────────────────────────────────────────────────
            when (selectedTab.value) {
                0 -> PlanComptableTab(
                    state = planState,
                    onSearchChange = planViewModel::onSearchQueryChange,
                    onToggleClasse = planViewModel::toggleClasse,
                    onToggleSousGroupe = planViewModel::toggleSousGroupe,
                    onRetry = planViewModel::load,
                    onInitialiser = planViewModel::initialiserPlan,
                )
                1 -> SaisieTab(
                    state = saisieState,
                    onSelectJournal = saisieViewModel::selectJournal,
                    onSetDate = saisieViewModel::setDate,
                    onSetLibelle = saisieViewModel::setLibelle,
                    onSetReferencePiece = saisieViewModel::setReferencePiece,
                    onUpdateLigneNumero = saisieViewModel::updateLigneNumero,
                    onUpdateLigneLibelle = saisieViewModel::updateLigneLibelle,
                    onUpdateLigneDebit = saisieViewModel::updateLigneDebit,
                    onUpdateLigneCredit = saisieViewModel::updateLigneCredit,
                    onAddLigne = saisieViewModel::addLigne,
                    onRemoveLigne = saisieViewModel::removeLigne,
                    onSaveAsBrouillon = saisieViewModel::saveAsBrouillon,
                    onRequestValidation = saisieViewModel::requestValidation,
                    onDismissValidationDialog = saisieViewModel::dismissValidationDialog,
                    onConfirmValidation = saisieViewModel::confirmValidation,
                    onDeleteBrouillon = saisieViewModel::deleteBrouillon,
                    onDismissSuccess = saisieViewModel::dismissSuccess,
                    onRetry = saisieViewModel::load,
                )
                2 -> JournauxTab(
                    state = journauxState,
                    onRetry = journauxViewModel::load,
                    onSetTypeFilter = journauxViewModel::setTypeFilter,
                    onToggleJournal = journauxViewModel::toggleJournal,
                    onShowCreateDialog = journauxViewModel::showCreateDialog,
                    onDismissCreateDialog = journauxViewModel::dismissCreateDialog,
                    onSetCreateCode = journauxViewModel::setCreateCode,
                    onSetCreateLibelle = journauxViewModel::setCreateLibelle,
                    onSetCreateType = journauxViewModel::setCreateType,
                    onCreateJournal = journauxViewModel::createJournal,
                )
                3 -> ExercicesTab(
                    state = exercicesState,
                    onRetry = exercicesViewModel::load,
                    onRequestCloturer = exercicesViewModel::requestCloturer,
                    onDismissCloturerDialog = exercicesViewModel::dismissCloturerDialog,
                    onConfirmCloturer = exercicesViewModel::confirmCloturer,
                    onDismissCloturerResult = exercicesViewModel::dismissCloturerResult,
                    onShowCreateDialog = exercicesViewModel::showCreateDialog,
                    onDismissCreateDialog = exercicesViewModel::dismissCreateDialog,
                    onSetCreateAnnee = exercicesViewModel::setCreateAnnee,
                    onSetCreateDateDebut = exercicesViewModel::setCreateDateDebut,
                    onSetCreateDateFin = exercicesViewModel::setCreateDateFin,
                    onCreateExercice = exercicesViewModel::createExercice,
                )
                4 -> EcrituresTab(
                    state = ecrituresState,
                    onRetry = ecrituresViewModel::load,
                    onSetJournalFilter = ecrituresViewModel::setJournalCodeFilter,
                    onToggleEcriture = ecrituresViewModel::toggleEcriture,
                )
                5 -> BalanceDesComptesTab(
                    state = balanceState,
                    onRetry = balanceViewModel::load,
                    onSelectExercice = balanceViewModel::selectExercice,
                    onSetSearchQuery = balanceViewModel::setSearchQuery,
                    onSetClasseFilter = balanceViewModel::setClasseFilter,
                    onSetSoldeFilter = balanceViewModel::setSoldeFilter,
                    onToggleShowZero = balanceViewModel::toggleShowZero,
                    onRefresh = balanceViewModel::loadBalance,
                )
                7 -> DeclarationTVATab(
                    state = tvaState,
                    onRetry = tvaViewModel::load,
                    onSetPeriodeMode = tvaViewModel::setPeriodeMode,
                    onSelectYear = tvaViewModel::selectYear,
                    onSelectMonth = tvaViewModel::selectMonth,
                    onSelectTrimestre = tvaViewModel::selectTrimestre,
                    onRefresh = tvaViewModel::loadDeclaration,
                )
                6 -> EtatsFinanciersTab(
                    state = etatsState,
                    onRetry = etatsViewModel::load,
                    onSelectEtat = etatsViewModel::selectEtat,
                    onSelectExercice = etatsViewModel::selectExercice,
                    onSetGLCompteNumero = etatsViewModel::setGLCompteNumero,
                    onRefreshBalance = etatsViewModel::loadBalance,
                    onRefreshBilan = etatsViewModel::loadBilan,
                    onRefreshResultat = etatsViewModel::loadResultat,
                )
                8 -> ImmobilisationsTab(
                    viewModel = immoViewModel,
                    state = immoState,
                )
                9 -> AvoirsTab(viewModel = avoirsViewModel)
            }
        }
    }
}

// ── Tab bar ───────────────────────────────────────────────────────────────────

@Composable
private fun ComptaTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit, nbEcrituresEnAttente: Int = 0) {
    androidx.compose.foundation.layout.Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TabPill(label = "Plan comptable", icon = Icons.AutoMirrored.Filled.List, selected = selectedTab == 0, onClick = { onTabSelected(0) })
            TabPill(label = "Saisie", icon = Icons.Filled.Edit, selected = selectedTab == 1, onClick = { onTabSelected(1) })
            TabPill(label = "Journaux", icon = Icons.Filled.AccountBalance, selected = selectedTab == 2, onClick = { onTabSelected(2) })
            TabPill(label = "Exercices", icon = Icons.Filled.History, selected = selectedTab == 3, onClick = { onTabSelected(3) })
            TabPillWithBadge(label = "Écritures", icon = Icons.AutoMirrored.Filled.ReceiptLong, selected = selectedTab == 4, onClick = { onTabSelected(4) }, count = nbEcrituresEnAttente)
            TabPill(label = "Balance", icon = Icons.Filled.BarChart, selected = selectedTab == 5, onClick = { onTabSelected(5) })
            TabPill(label = "États", icon = Icons.AutoMirrored.Filled.TrendingUp, selected = selectedTab == 6, onClick = { onTabSelected(6) })
            TabPill(label = "Déclaration TVA", icon = Icons.Filled.Percent, selected = selectedTab == 7, onClick = { onTabSelected(7) })
            TabPill(label = "Immobilisations", icon = Icons.Filled.Wallet, selected = selectedTab == 8, onClick = { onTabSelected(8) })
            TabPill(label = "Avoirs", icon = Icons.AutoMirrored.Filled.ReceiptLong, selected = selectedTab == 9, onClick = { onTabSelected(9) })
        }
        HorizontalDivider(color = FormulooOutline, thickness = 0.5.dp)
    }
}

@Composable
private fun TabPill(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) FormulooPrimary else Color.Transparent
    val contentColor = if (selected) Color.White else FormulooOnSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
    }
}

@Composable
private fun TabPillWithBadge(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, count: Int = 0) {
    val bgColor = if (selected) FormulooPrimary else Color.Transparent
    val contentColor = if (selected) Color.White else FormulooOnSurfaceVariant
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
            )
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(FormulooSecondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ── Plan comptable tab ────────────────────────────────────────────────────────

@Composable
private fun PlanComptableTab(
    state: PlanComptableUiState,
    onSearchChange: (String) -> Unit,
    onToggleClasse: (Int) -> Unit,
    onToggleSousGroupe: (String) -> Unit,
    onRetry: () -> Unit,
    onInitialiser: () -> Unit,
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FormulooPrimary)
        }
        state.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error, color = FormulooError, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.TextButton(onClick = onRetry) {
                    Text("Réessayer", color = FormulooPrimary)
                }
            }
        }
        state.isEmpty -> PlanVideState(
            isInitialising = state.isInitialising,
            initialiseError = state.initialiseError,
            onInitialiser = onInitialiser,
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // Stats cards
            item {
                state.stats?.let { PlanStatsRow(it) }
            }

            // Search bar
            item {
                PlanSearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchChange,
                )
            }

            // Hierarchical list — sous-groupes toujours visibles quand la classe est expandée
            state.filteredClasses.forEach { classeGroup ->
                item(key = "cl_${classeGroup.numero}") {
                    ClasseHeader(
                        group = classeGroup,
                        isExpanded = state.expandedClasses.contains(classeGroup.numero),
                        onToggle = { onToggleClasse(classeGroup.numero) },
                    )
                }

                if (state.expandedClasses.contains(classeGroup.numero)) {
                    classeGroup.sousGroupes.forEach { sg ->
                        item(key = "sg_${sg.prefixe}") {
                            SousGroupeHeader(group = sg)
                        }
                        items(sg.comptes, key = { "cpt_${it.numero}" }) { compte ->
                            CompteRow(compte)
                        }
                    }
                }
            }

            if (state.filteredClasses.isEmpty() && state.searchQuery.isNotBlank()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Aucun compte trouvé pour « ${state.searchQuery} »",
                            color = FormulooOnSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // Bannière info en bas de liste
            item {
                state.stats?.let { PlanInfoBanner(it) }
            }
        }
    }
}

@Composable
private fun PlanStatsRow(stats: PlanComptableStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlanStatCard(
            label = "Comptes actifs",
            value = stats.nbComptesActifs.toString(),
            subLabel = "Plan SYSCOHADA",
            iconBg = FormulooMint,
            iconTint = FormulooPrimary,
            icon = Icons.AutoMirrored.Filled.List,
        )
        PlanStatCard(
            label = "Total actif",
            value = "${formatCompact(stats.totalActif)} F",
            subLabel = "au ${stats.dateAt}",
            iconBg = FormulooMint,
            iconTint = FormulooPrimary,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
        )
        PlanStatCard(
            label = "Trésorerie nette",
            value = "${formatCompact(stats.tresorerieNette)} F",
            subLabel = "banques + caisse",
            iconBg = FormulooSecondaryBg,
            iconTint = FormulooSecondary,
            icon = Icons.Filled.Wallet,
        )
    }
}

@Composable
private fun PlanStatCard(
    label: String,
    value: String,
    subLabel: String,
    iconBg: Color,
    iconTint: Color,
    icon: ImageVector,
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

@Composable
private fun PlanSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FormulooSurface)
            .border(1.dp, FormulooOutline, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = FormulooOnSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "Rechercher un compte (code ou libellé)..",
                        color = FormulooOnSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun PlanInfoBanner(stats: PlanComptableStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp)
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
                "Plan comptable SYSCOHADA révisé",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooPrimary,
            )
            Spacer(Modifier.height(3.dp))
            val year = stats.exerciceAnnee?.toString() ?: "—"
            Text(
                "7 classes, du bilan (1–5) au compte de résultat (6–7). Les soldes sont arrêtés au ${stats.dateAt} · Exercice $year.",
                fontSize = 12.sp,
                color = FormulooPrimary.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun ClasseHeader(group: ClasseGroup, isExpanded: Boolean, onToggle: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooBackground)
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = FormulooOnSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                // Pilule badge mint "Cl. N"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(FormulooMint)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "Cl. ${group.numero}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = FormulooPrimary,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    group.libelle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = FormulooTextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (group.total > 0) {
                Text(
                    "${formatCompact(group.total)} F",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = FormulooTextPrimary,
                )
            }
        }
        HorizontalDivider(color = FormulooOutline, thickness = 0.5.dp)
    }
}

@Composable
private fun SousGroupeHeader(group: SousGroupeGroup) {
    Column {
        HorizontalDivider(color = FormulooOutline.copy(alpha = 0.6f), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooSurface)
                .padding(start = 46.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${group.prefixe} · ${group.libelle.uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = FormulooOnSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sgNet = group.total
            if (sgNet != 0.0) {
                Text(
                    formatAmount(kotlin.math.abs(sgNet)),
                    fontSize = 11.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompteRow(compte: com.formuloo.feature.compta.domain.model.CompteItemUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(start = 64.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                compte.numero,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = FormulooPrimary,
                modifier = Modifier.width(44.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                compte.libelle,
                fontSize = 13.sp,
                color = FormulooOnSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (compte.soldeAbsolu > 0) {
            Text(
                formatAmount(compte.soldeAbsolu),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooTextPrimary,
            )
        }
    }
}

@Composable
private fun SoldeBadge(isDebiteur: Boolean) {
    val label = if (isDebiteur) "D" else "C"
    val bg = if (isDebiteur) FormulooMint else FormulooSecondaryBg
    val tint = if (isDebiteur) FormulooPrimary else FormulooSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

// ── Empty plan state ──────────────────────────────────────────────────────────

@Composable
private fun PlanVideState(
    isInitialising: Boolean,
    initialiseError: String?,
    onInitialiser: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(FormulooMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = FormulooPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Plan comptable vide",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = FormulooTextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Initialisez le plan comptable SYSCOHADA pour créer\nautomatiquement 130+ comptes standard OHADA,\nles journaux (VTE, ACH, BNQ, CAI, OD) et l'exercice courant.",
                fontSize = 13.sp,
                color = FormulooOnSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            if (isInitialising) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = FormulooPrimary)
                    Text("Initialisation en cours...", fontSize = 14.sp, color = FormulooPrimary)
                }
            } else {
                androidx.compose.material3.Button(
                    onClick = onInitialiser,
                    shape = RoundedCornerShape(10.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
                ) {
                    Text("Initialiser le plan SYSCOHADA", fontWeight = FontWeight.SemiBold)
                }
            }

            if (initialiseError != null) {
                Spacer(Modifier.height(12.dp))
                Text(initialiseError, color = FormulooError, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

internal fun formatAmount(value: Double): String {
    val rounded = value.toLong()
    val s = rounded.toString()
    val sb = StringBuilder()
    for ((index, char) in s.reversed().withIndex()) {
        if (index != 0 && index % 3 == 0) sb.append(' ')
        sb.append(char)
    }
    return sb.reverse().toString()
}

private fun formatCompact(value: Double): String = when {
    value >= 1_000_000_000 -> "${(value / 1_000_000_000).toLong()} Md"
    value >= 1_000_000 -> "${(value / 1_000_000).toLong()} M"
    value >= 1_000 -> "${(value / 1_000).toLong()} k"
    else -> formatAmount(value)
}
