package com.formuloo.feature.gesdoc.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooBlue
import com.formuloo.core.designsystem.FormulooBlueBg
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTeal
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.FormulooVersionGray
import com.formuloo.feature.gesdoc.domain.model.AuditAction
import com.formuloo.feature.gesdoc.domain.model.AuditActorType
import com.formuloo.feature.gesdoc.domain.model.AuditLogEntry
import com.formuloo.feature.gesdoc.presentation.components.GesDocTabBar
import com.formuloo.feature.gesdoc.presentation.viewmodel.AuditCategory
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocAuditViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesDocAuditScreen(
    onBack: () -> Unit,
    onNavigateToUpload: () -> Unit = {},
    viewModel: GesDocAuditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.hasMore, state.isLoadingMore) {
        if (shouldLoadMore && state.hasMore && !state.isLoadingMore) {
            viewModel.loadMore()
        }
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
                    IconButton(onClick = {}) {
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
                activeTabIndex = 2,
                onNavigateToDashboard = onBack,
                onNavigateToUpload = onNavigateToUpload,
                onNavigateToAudit = {},
            )

            // Chips de filtre par catégorie
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AuditCategory.entries.forEach { category ->
                    CategoryChip(
                        label = category.label,
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                    )
                }
            }

            // Carte "Journal d'audit"
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = FormulooSurface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Journal d'audit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = FormulooTextPrimary,
                            )
                            Text(
                                "Chronologique · lecture seule",
                                fontSize = 12.sp,
                                color = FormulooOnSurfaceVariant,
                            )
                        }
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                color = FormulooPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            IconButton(onClick = viewModel::exportLog) {
                                Icon(
                                    Icons.Filled.Download,
                                    contentDescription = "Exporter le journal",
                                    tint = FormulooTextPrimary,
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = FormulooOutline.copy(alpha = 0.5f), thickness = 0.5.dp)

                    if (state.isLoading && state.entries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = FormulooPrimary)
                        }
                    } else if (state.entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Aucun événement pour cette catégorie.",
                                fontSize = 13.sp,
                                color = FormulooOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
                                AuditEventRow(entry = entry)
                                if (index < state.entries.lastIndex || state.hasMore) {
                                    HorizontalDivider(color = FormulooOutline.copy(alpha = 0.5f), thickness = 0.5.dp)
                                }
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            color = FormulooPrimary,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.error != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = FormulooError,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(state.error!!, fontSize = 13.sp, color = FormulooError)
                        }
                    }
                }
            }
        }
    }
}

// ── Chip de filtre par catégorie ──────────────────────────────────────────────

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) FormulooPrimary else FormulooSurface
    val contentColor = if (selected) Color.White else FormulooOnSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else FormulooOutline,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
    }
}

// ── Ligne d'un événement du journal ───────────────────────────────────────────

@Composable
private fun AuditEventRow(entry: AuditLogEntry) {
    val style = auditEventStyle(entry.action)
    val author = when (entry.actorType) {
        AuditActorType.SYSTEM -> "Système"
        AuditActorType.USER -> "Utilisateur"
        AuditActorType.UNKNOWN -> "—"
    }
    val detailLine = listOfNotNull(
        entry.documentNumber?.takeIf { it.isNotBlank() },
        entry.detail.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(style.badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.label,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = FormulooTextPrimary,
            )
            if (detailLine.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    detailLine,
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "par $author",
                fontSize = 11.sp,
                color = FormulooVersionGray,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatShortTimestamp(entry.timestamp),
            fontSize = 11.sp,
            color = FormulooOnSurfaceVariant,
        )
    }
}

// ── Mapping icône / couleur par type d'événement ─────────────────────────────

private data class AuditEventStyle(
    val badgeBg: Color,
    val iconTint: Color,
    val icon: ImageVector,
)

private fun auditEventStyle(action: AuditAction): AuditEventStyle = when (action) {
    AuditAction.CERTIFIED -> AuditEventStyle(FormulooMint, FormulooTeal, Icons.Filled.VerifiedUser)
    AuditAction.OCR_VALIDATED -> AuditEventStyle(FormulooMint, FormulooTeal, Icons.Filled.CheckCircle)
    AuditAction.INTEGRITY_CHECK -> AuditEventStyle(FormulooMint, FormulooTeal, Icons.Filled.Fingerprint)
    AuditAction.OCR_EXTRACTED -> AuditEventStyle(FormulooBlueBg, FormulooBlue, Icons.Filled.Description)
    AuditAction.JOURNAL_LINKED -> AuditEventStyle(FormulooBlueBg, FormulooBlue, Icons.Filled.Link)
    AuditAction.OCR_CORRECTION -> AuditEventStyle(FormulooBlueBg, FormulooBlue, Icons.Filled.Edit)
    AuditAction.UPLOAD -> AuditEventStyle(FormulooSecondaryBg, FormulooSecondary, Icons.Filled.Add)
    AuditAction.INTEGRITY_ALERT -> AuditEventStyle(FormulooErrorBg, FormulooError, Icons.Filled.Warning)
    AuditAction.UNKNOWN -> AuditEventStyle(FormulooOutline, FormulooOnSurfaceVariant, Icons.Filled.Info)
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatShortTimestamp(iso: String): String {
    val monthNames = listOf(
        "janv.", "févr.", "mars", "avr.", "mai", "juin",
        "juil.", "août", "sept.", "oct.", "nov.", "déc.",
    )
    return try {
        val (datePart, timePartRaw) = iso.split("T")
        val (_, month, day) = datePart.split("-")
        val timePart = timePartRaw.trimEnd('Z').substringBefore(".").take(5)
        val monthLabel = monthNames.getOrNull(month.toInt() - 1) ?: month
        "${day.padStart(2, '0')} $monthLabel · $timePart"
    } catch (_: Exception) {
        iso
    }
}
