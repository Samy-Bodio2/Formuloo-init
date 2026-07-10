package com.formuloo.feature.gesdoc.presentation.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTeal
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.StepProgressBar
import com.formuloo.feature.gesdoc.domain.model.BlockchainStatus
import com.formuloo.feature.gesdoc.presentation.components.GesDocTabBar
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocCertificationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesDocCertificationScreen(
    documentId: String,
    onBack: () -> Unit,
    onNavigateToAudit: () -> Unit = {},
    onNavigateToCompta: () -> Unit,
    viewModel: GesDocCertificationViewModel = koinViewModel(parameters = { parametersOf(documentId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

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
        bottomBar = {
            CertificationFooter(onNavigateToCompta = onNavigateToCompta)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GesDocTabBar(
                activeTabIndex = 1,
                onNavigateToDashboard = onBack,
                onNavigateToUpload = {},
                onNavigateToAudit = onNavigateToAudit,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // Barre de progression 4 étapes
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        StepProgressBar(totalSteps = 4, currentStep = 4)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Étape 4 sur 4 · Certification",
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }

                // Zone de confirmation centrée
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Document certifié ✓",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = FormulooTextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Le hash SHA-256 de la pièce a été calculé et ancré sur Ethereum. " +
                                "Toute modification ultérieure du fichier sera détectable.",
                            fontSize = 13.sp,
                            color = FormulooOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Carte des données de certification blockchain
                item {
                    Spacer(Modifier.height(24.dp))
                    CertificationCard(
                        isLoading = state.isLoading && state.blockchainStatus == null,
                        status = state.blockchainStatus,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                // Bouton "Voir la preuve Blockchain"
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { state.explorerUrl?.let(uriHandler::openUri) },
                        enabled = state.explorerUrl != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, FormulooTeal),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FormulooTeal),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Voir la preuve Blockchain",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }

                // Bannière erreur
                if (state.error != null) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(FormulooErrorBg, RoundedCornerShape(10.dp))
                                .padding(12.dp),
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

// ── Carte des données de certification blockchain ────────────────────────────
// Reprend exactement les tokens de la carte blockchain de DocumentDetailScreen
// (DetailSurface / BlockchainInfoRow / badges mint) — composables privés à cet
// autre fichier, donc répliqués ici avec les mêmes styles plutôt qu'importés.

@Composable
private fun CertificationCard(
    isLoading: Boolean,
    status: BlockchainStatus?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = FormulooPrimary, modifier = Modifier.size(24.dp))
                }
                status == null -> Text(
                    "Informations blockchain non disponibles",
                    fontSize = 13.sp,
                    color = FormulooOnSurfaceVariant,
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (status.hashSha256 != null) {
                        CertificationDataRow(
                            badge = { HashSymbolBadge() },
                            label = "EMPREINTE SHA-256",
                            value = status.hashSha256,
                            monospace = true,
                            selectable = true,
                        )
                    }
                    if (status.txHash != null) {
                        CertificationDataRow(
                            badge = { MintIconBadge(icon = Icons.Filled.Link) },
                            label = "TRANSACTION ETHEREUM",
                            value = buildTxLine(status.txHash, status.blockNumber),
                            monospace = true,
                            selectable = true,
                        )
                    }
                    if (status.anchoredAt != null) {
                        CertificationDataRow(
                            badge = { MintIconBadge(icon = Icons.Filled.AccessTime) },
                            label = "HORODATAGE",
                            value = formatTimestamp(status.anchoredAt),
                            monospace = false,
                            selectable = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HashSymbolBadge() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FormulooMint),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "#",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = FormulooTeal,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun MintIconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FormulooMint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FormulooTeal,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CertificationDataRow(
    badge: @Composable () -> Unit,
    label: String,
    value: String,
    monospace: Boolean,
    selectable: Boolean,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        badge()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooLabelGray,
                letterSpacing = 0.8.sp,
            )
            val valueContent: @Composable () -> Unit = {
                Text(
                    value,
                    fontSize = 13.sp,
                    color = if (monospace) FormulooTeal else Color(0xFF424242),
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = if (monospace) FontWeight.Medium else FontWeight.Normal,
                )
            }
            if (selectable) {
                SelectionContainer { valueContent() }
            } else {
                valueContent()
            }
        }
    }
}

// ── Footer fixe : uniquement le bouton principal (pas de retour) ────────────

@Composable
private fun CertificationFooter(onNavigateToCompta: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = onNavigateToCompta,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
        ) {
            Text(
                "Aller au module Comptabilité",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildTxLine(txHash: String, blockNumber: Int?): String {
    val truncated = if (txHash.length > 14) {
        "${txHash.take(6)}…${txHash.takeLast(5)}"
    } else {
        txHash
    }
    return if (blockNumber != null) "$truncated · bloc #${formatBlockNumber(blockNumber)}" else truncated
}

private fun formatBlockNumber(n: Int): String {
    val sb = StringBuilder()
    n.toString().reversed().forEachIndexed { i, c ->
        if (i > 0 && i % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return sb.toString().reversed()
}

private fun formatTimestamp(iso: String): String {
    val monthNames = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    )
    return try {
        val (datePart, timePartRaw) = iso.split("T")
        val (year, month, day) = datePart.split("-")
        val timePart = timePartRaw.trimEnd('Z').substringBefore(".").take(8)
        val monthLabel = monthNames.getOrNull(month.toInt() - 1) ?: month
        "${day.padStart(2, '0')} $monthLabel $year · $timePart UTC"
    } catch (_: Exception) {
        iso
    }
}
