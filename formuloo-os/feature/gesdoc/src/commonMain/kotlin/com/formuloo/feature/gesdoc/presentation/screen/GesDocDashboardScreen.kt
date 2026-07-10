package com.formuloo.feature.gesdoc.presentation.screen

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooBlue
import com.formuloo.core.designsystem.FormulooBlueBg
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooGreen
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooPurple
import com.formuloo.core.designsystem.FormulooPurpleBg
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTeal
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.FormulooVersionGray
import com.formuloo.feature.gesdoc.domain.model.DocumentStatus
import com.formuloo.feature.gesdoc.domain.model.DocumentSummary
import com.formuloo.feature.gesdoc.domain.model.DocumentType
import com.formuloo.feature.gesdoc.domain.model.GesDocStats
import com.formuloo.feature.gesdoc.presentation.components.GesDocTabBar
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocDashboardViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesDocDashboardScreen(
    onBack: () -> Unit,
    onNavigateToDocument: (String) -> Unit,
    onNavigateToUpload: () -> Unit = {},
    onNavigateToAudit: () -> Unit = {},
    viewModel: GesDocDashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filtered = viewModel.filterDocuments(state)

    // Recharge la liste à chaque retour sur cet écran (ex. après upload/validation/
    // certification d'une pièce) — sans ça, la même instance de ViewModel restait
    // scopée au back-stack entry et n'affichait que l'état chargé au tout premier accès.
    LifecycleResumeEffect(Unit) {
        viewModel.loadDocuments()
        onPauseOrDispose { }
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Gestion documentaire",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FormulooTextPrimary,
                        )
                        Text(
                            "OCR · certification Blockchain",
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
                    IconButton(onClick = { /* TODO: bottom sheet filtres avancés */ }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filtrer",
                            tint = FormulooOnSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooSurface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GesDocTabBar(
                activeTabIndex = 0,
                onNavigateToDashboard = {},
                onNavigateToUpload = onNavigateToUpload,
                onNavigateToAudit = onNavigateToAudit,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item { GesDocStatsRow(stats = state.stats) }

                item {
                    GesDocSearchBar(
                        query = state.searchQuery,
                        onQueryChange = viewModel::setSearchQuery,
                    )
                }

                item {
                    GesDocFilterRow(
                        selectedFilter = state.selectedFilter,
                        onSelect = viewModel::selectFilter,
                    )
                }

                when {
                    state.isLoading -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = FormulooPrimary)
                        }
                    }
                    state.error != null -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                state.error!!,
                                color = FormulooError,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                        }
                    }
                    filtered.isEmpty() -> item { GesDocEmptyState() }
                    else -> items(filtered, key = { it.id }) { doc ->
                        DocumentCard(doc = doc, onClick = { onNavigateToDocument(doc.id) })
                    }
                }
            }
        }
    }
}

// ── Cartes de statistiques ────────────────────────────────────────────────────

@Composable
private fun GesDocStatsRow(stats: GesDocStats) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            GesDocStatCard(
                icon = Icons.Filled.Description,
                iconBg = FormulooMint,
                iconTint = FormulooTeal,
                value = stats.totalArchived.toString(),
                label = "Pièces archivées",
                subLabel = "exercice 2025",
            )
        }
        item {
            GesDocStatCard(
                icon = Icons.Filled.Security,
                iconBg = FormulooMint,
                iconTint = FormulooGreen,
                value = stats.certifiedOnChain.toString(),
                label = "Certifiées on-chain",
                subLabel = "Ethereum",
            )
        }
        item {
            GesDocStatCard(
                icon = Icons.Filled.HourglassEmpty,
                iconBg = FormulooSecondaryBg,
                iconTint = FormulooSecondary,
                value = stats.inProcessing.toString(),
                label = "En traitement",
                subLabel = "OCR en cours",
            )
        }
        item {
            GesDocStatCard(
                icon = Icons.Filled.Warning,
                iconBg = FormulooErrorBg,
                iconTint = FormulooError,
                value = stats.integrityAlerts.toString(),
                label = "Alerte intégrité",
                subLabel = "hash divergent",
            )
        }
    }
}

@Composable
private fun GesDocStatCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    label: String,
    subLabel: String,
) {
    Card(
        modifier = Modifier.width(150.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = FormulooTextPrimary,
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = FormulooTextPrimary,
            )
            Text(
                text = subLabel,
                fontSize = 11.sp,
                color = FormulooLabelGray,
            )
        }
    }
}

// ── Barre de recherche ────────────────────────────────────────────────────────

@Composable
private fun GesDocSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Rechercher une pièce (n° ou fournisseur).",
                fontSize = 14.sp,
                color = FormulooLabelGray,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = FormulooLabelGray,
                modifier = Modifier.size(20.dp),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = FormulooSurface,
            focusedContainerColor = FormulooSurface,
            unfocusedBorderColor = FormulooOutline,
            focusedBorderColor = FormulooPrimary,
            cursorColor = FormulooPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

// ── Chips de filtre rapide ────────────────────────────────────────────────────

@Composable
private fun GesDocFilterRow(selectedFilter: Int, onSelect: (Int) -> Unit) {
    val filters = listOf("Tous", "En cours", "Certifiés", "Alertes")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEachIndexed { index, label ->
            val selected = selectedFilter == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) FormulooPrimary else FormulooSurface)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Color.White else FormulooOnSurfaceVariant,
                )
            }
        }
    }
}

// ── Carte de document ─────────────────────────────────────────────────────────

@Composable
private fun DocumentCard(doc: DocumentSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DocumentTypeIconBadge(documentType = doc.documentType)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = doc.number ?: "—",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = FormulooTextPrimary,
                )
                Text(
                    text = buildSupplierAmountLine(doc),
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DocumentStatusPill(status = doc.status)
                    if (doc.status == DocumentStatus.CERTIFIED) {
                        OnChainBadge()
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = FormulooOutline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DocumentTypeIconBadge(documentType: DocumentType) {
    val (bg, icon, tint) = when (documentType) {
        DocumentType.INVOICE -> Triple(FormulooMint, Icons.Filled.Description, FormulooTeal)
        DocumentType.PURCHASE_ORDER -> Triple(FormulooBlueBg, Icons.Filled.ShoppingCart, FormulooBlue)
        DocumentType.RECEIPT -> Triple(FormulooSecondaryBg, Icons.Filled.CreditCard, FormulooSecondary)
        DocumentType.UNKNOWN -> Triple(
            Color(0xFFF0F0F0),
            Icons.Filled.Description,
            FormulooVersionGray,
        )
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = documentType.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun DocumentStatusPill(status: DocumentStatus) {
    val (bg, dotColor, textColor, label) = when (status) {
        DocumentStatus.CERTIFIED ->
            StatusPillStyle(FormulooMint, FormulooGreen, Color(0xFF2E7D32), "Certifié")
        DocumentStatus.EXTRACTED, DocumentStatus.VALIDATED ->
            StatusPillStyle(FormulooBlueBg, FormulooBlue, Color(0xFF1565C0), "À valider")
        DocumentStatus.PENDING_CHAIN ->
            StatusPillStyle(FormulooPurpleBg, FormulooPurple, Color(0xFF6A1B9A), "Blockchain…")
        DocumentStatus.TAMPERED ->
            StatusPillStyle(FormulooErrorBg, FormulooError, FormulooError, "Alerte")
        DocumentStatus.FAILED ->
            StatusPillStyle(FormulooErrorBg, FormulooError, FormulooError, "Échec")
        else ->
            StatusPillStyle(FormulooSecondaryBg, FormulooSecondary, Color(0xFFE65100), "En cours")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}

private data class StatusPillStyle(
    val bg: Color,
    val dotColor: Color,
    val textColor: Color,
    val label: String,
)

@Composable
private fun OnChainBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Security,
            contentDescription = null,
            tint = FormulooLabelGray,
            modifier = Modifier.size(11.dp),
        )
        Text(
            text = "on-chain",
            fontSize = 10.sp,
            color = FormulooLabelGray,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── État vide ─────────────────────────────────────────────────────────────────

@Composable
private fun GesDocEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(FormulooMint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CloudUpload,
                contentDescription = null,
                tint = FormulooTeal,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Aucun document archivé",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = FormulooTextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Déposez une facture ou un reçu\npour démarrer le pipeline OCR.",
            fontSize = 13.sp,
            color = FormulooOnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildSupplierAmountLine(doc: DocumentSummary): String {
    val supplier = doc.supplier ?: doc.documentType.label
    return if (doc.amountTtc != null) {
        "$supplier · ${formatAmount(doc.amountTtc, doc.currency)}"
    } else {
        supplier
    }
}

private fun formatAmount(value: Double, currency: String): String {
    val formatted = when {
        value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)} M"
        value >= 1_000 -> "${"%.0f".format(value / 1_000)} k"
        else -> "%.0f".format(value)
    }
    return "$formatted $currency"
}
