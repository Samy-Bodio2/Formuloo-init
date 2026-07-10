package com.formuloo.feature.gesdoc.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.formuloo.feature.gesdoc.domain.model.AccountingPrefill
import com.formuloo.feature.gesdoc.domain.model.BlockchainStatus
import com.formuloo.feature.gesdoc.domain.model.DocumentStatus
import com.formuloo.feature.gesdoc.presentation.viewmodel.DocumentDetailState
import com.formuloo.feature.gesdoc.presentation.viewmodel.DocumentDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    onBack: () -> Unit,
    onNavigateToOriginal: (String) -> Unit = {},
    viewModel: DocumentDetailViewModel = koinViewModel(parameters = { parametersOf(documentId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val docNumber = state.ocrResult?.fields?.get("document_number")?.value
        ?: documentId.take(12).uppercase()
    val docStatus = state.blockchainStatus?.status ?: state.ocrResult?.status
    val hashOnChain = state.blockchainStatus?.hashSha256
    val hashRecomputed = state.integrityResult?.computedHash
    val integrityOk = state.integrityResult?.integrityOk
    val hasVerified = state.integrityResult != null
    val canVerify = !hasVerified && (docStatus == DocumentStatus.CERTIFIED || docStatus == DocumentStatus.TAMPERED)

    val subtitle = when {
        state.integrityResult != null && integrityOk == false -> "Alerte · intégrité compromise"
        docStatus == DocumentStatus.CERTIFIED -> "Pièce certifiée · intégrité"
        docStatus == DocumentStatus.TAMPERED -> "Alerte · intégrité compromise"
        docStatus == DocumentStatus.PENDING_CHAIN -> "Ancrage blockchain en cours…"
        else -> "Détails du document"
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            docNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FormulooTextPrimary,
                        )
                        Text(
                            subtitle,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ① Document identity
            item { DocumentIdentityCard(state = state, docNumber = docNumber) }

            // Bouton "Voir le document original" — visible dès qu'un fichier consultable existe
            if (state.documentDetail?.viewableUrl != null) {
                item {
                    ViewOriginalButton(onClick = { onNavigateToOriginal(documentId) })
                }
            }

            // ② Blockchain on-chain info
            item {
                BlockchainInfoCard(
                    isLoading = state.isLoadingBlockchain,
                    status = state.blockchainStatus,
                )
            }

            // ③ État A — avant vérification : bouton plein largeur, hors carte
            if (canVerify) {
                item {
                    VerifyIntegrityButton(
                        isLoading = state.isVerifyingIntegrity,
                        onClick = viewModel::verifyIntegrity,
                    )
                }
            }

            // ③ État B — après vérification : bannière + cartes de hash, en fondu
            if (hasVerified) {
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn(tween(350))) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AuthenticityBanner(authentic = integrityOk == true)
                            if (hashOnChain != null) {
                                HashCard(
                                    label = "HASH ON-CHAIN",
                                    icon = Icons.Filled.Security,
                                    hash = hashOnChain,
                                    isError = false,
                                )
                            }
                            if (hashRecomputed != null) {
                                HashCard(
                                    label = "HASH RECALCULÉ",
                                    icon = Icons.Filled.Refresh,
                                    hash = hashRecomputed,
                                    isError = integrityOk == false,
                                )
                            }
                        }
                    }
                }
            }

            // ⑥ Suggestion comptable SYSCOHADA
            val canSyscohada = docStatus == DocumentStatus.CERTIFIED
            if (canSyscohada) {
                item {
                    SyscohadaButton(onClick = viewModel::loadPrefill)
                }
            }

            // ⑦ SYSCOHADA prefill
            if (state.prefill != null) {
                item { PrefillSection(prefill = state.prefill!!) }
            }

            // ⑧ Action success banner
            if (state.actionSuccess != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = FormulooGreen,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(state.actionSuccess!!, fontSize = 13.sp, color = Color(0xFF2E7D32))
                    }
                }
            }

            // ⑨ Error banner
            if (state.error != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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

// ── ① Document identity card ──────────────────────────────────────────────────

@Composable
private fun DocumentIdentityCard(state: DocumentDetailState, docNumber: String) {
    DetailSurface {
        if (state.isLoadingOcr && state.ocrResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FormulooPrimary, modifier = Modifier.size(24.dp))
            }
        } else {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FormulooMint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = FormulooTeal,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        docNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = FormulooTextPrimary,
                    )
                    val supplier = state.ocrResult?.fields?.get("supplier")?.value
                    val amount = state.ocrResult?.fields?.get("amount_ttc")?.value
                    val subLine = buildList {
                        if (supplier != null) add(supplier)
                        if (amount != null) add("$amount FCFA")
                    }.joinToString(" · ")
                    if (subLine.isNotEmpty()) {
                        Text(
                            subLine,
                            fontSize = 13.sp,
                            color = FormulooOnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val status = state.blockchainStatus?.status ?: state.ocrResult?.status
                    if (status != null) {
                        Spacer(Modifier.height(2.dp))
                        DetailStatusPill(status = status)
                    }
                }
            }
        }
    }
}

// ── ② Blockchain on-chain info card ──────────────────────────────────────────

@Composable
private fun BlockchainInfoCard(isLoading: Boolean, status: BlockchainStatus?) {
    DetailSurface {
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
                    BlockchainInfoRow(
                        badge = { HashSymbolBadge() },
                        label = "HASH ENREGISTRÉ ON-CHAIN",
                        value = status.hashSha256,
                        selectable = true,
                    )
                }
                if (status.txHash != null) {
                    BlockchainInfoRow(
                        badge = {
                            MintIconBadge(icon = Icons.Filled.Link, description = "Transaction")
                        },
                        label = "TRANSACTION",
                        value = buildTxLine(status.txHash, status.blockNumber),
                        selectable = false,
                    )
                }
                // Status indicator when not yet certified / in progress
                if (status.status == DocumentStatus.PENDING_CHAIN) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.HourglassEmpty,
                            contentDescription = null,
                            tint = FormulooPurple,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "Ancrage Ethereum en cours…",
                            fontSize = 12.sp,
                            color = FormulooPurple,
                            fontWeight = FontWeight.Medium,
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
private fun MintIconBadge(icon: ImageVector, description: String) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FormulooMint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = FormulooTeal,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun BlockchainInfoRow(
    badge: @Composable () -> Unit,
    label: String,
    value: String,
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
            if (selectable) {
                SelectionContainer {
                    Text(
                        value,
                        fontSize = 13.sp,
                        color = FormulooTeal,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Text(
                    value,
                    fontSize = 13.sp,
                    color = FormulooTeal,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ── ③ Authenticity banner ─────────────────────────────────────────────────────

@Composable
private fun AuthenticityBanner(authentic: Boolean) {
    val bg = if (authentic) FormulooMint else FormulooErrorBg
    val badgeBg = if (authentic) FormulooTeal else FormulooError
    val icon = if (authentic) Icons.Filled.Check else Icons.Filled.PriorityHigh
    val title = if (authentic) "Document authentique" else "Intégrité compromise"
    val titleColor = if (authentic) FormulooTeal else FormulooError
    val body = if (authentic) {
        "Intégrité confirmée — le hash recalculé du fichier correspond exactement au hash ancré sur Ethereum."
    } else {
        "Le hash recalculé du fichier ne correspond pas au hash ancré sur Ethereum. Ce document a peut-être été modifié depuis sa certification."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = titleColor,
                )
                Text(
                    body,
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }
        }
    }
}

// ── ④ & ⑤ Hash comparison cards ──────────────────────────────────────────────

@Composable
private fun HashCard(
    label: String,
    icon: ImageVector,
    hash: String,
    isError: Boolean,
) {
    DetailSurface {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = FormulooLabelGray,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FormulooLabelGray,
                    letterSpacing = 0.8.sp,
                )
            }
            SelectionContainer {
                Text(
                    hash,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isError) FormulooError else Color(0xFF424242),
                )
            }
        }
    }
}

// ── Bouton "Voir le document original" ────────────────────────────────────────

@Composable
private fun ViewOriginalButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FormulooTextPrimary),
    ) {
        Icon(
            Icons.Filled.Description,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("Voir le document original", fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

// ── ⑥ Bouton "Vérifier l'intégrité" — État A, plein largeur, hors carte ──────

@Composable
private fun VerifyIntegrityButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FormulooPrimary,
            disabledContainerColor = FormulooPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text("Vérification...", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        } else {
            Icon(
                Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Vérifier l'intégrité", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }
    }
}

// ── Suggestion comptable SYSCOHADA (preserved behavior) ──────────────────────

@Composable
private fun SyscohadaButton(onClick: () -> Unit) {
    DetailSurface {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = FormulooTeal),
        ) {
            Icon(
                Icons.Filled.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("Suggestion comptable SYSCOHADA", fontSize = 13.sp)
        }
    }
}

// ── ⑦ SYSCOHADA prefill (preserved behavior) ─────────────────────────────────

@Composable
private fun PrefillSection(prefill: AccountingPrefill) {
    DetailSurface {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Icon(
                    Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = FormulooTeal,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Écriture SYSCOHADA suggérée",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = FormulooTextPrimary,
                )
            }
            prefill.lines.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            line.compte,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = FormulooTeal,
                        )
                        Text(
                            line.libelle,
                            fontSize = 11.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (line.debit > 0) {
                            Text(
                                "D ${formatAmount(line.debit, "XAF")}",
                                fontSize = 11.sp,
                                color = FormulooGreen,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (line.credit > 0) {
                            Text(
                                "C ${formatAmount(line.credit, "XAF")}",
                                fontSize = 11.sp,
                                color = FormulooError,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                HorizontalDivider(color = FormulooOutline)
            }
            if (prefill.confidence != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FormulooMint, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = FormulooTeal,
                        modifier = Modifier.size(14.dp),
                    )
                    Text("Confiance : ${prefill.confidence}", fontSize = 11.sp, color = FormulooTeal)
                }
            }
        }
    }
}

// ── Status pill (mirrors dashboard tokens) ────────────────────────────────────

private data class PillStyle(
    val bg: Color,
    val dotColor: Color,
    val textColor: Color,
    val label: String,
)

@Composable
private fun DetailStatusPill(status: DocumentStatus) {
    val style = when (status) {
        DocumentStatus.CERTIFIED ->
            PillStyle(FormulooMint, FormulooGreen, Color(0xFF2E7D32), "Certifié")
        DocumentStatus.EXTRACTED, DocumentStatus.VALIDATED ->
            PillStyle(FormulooBlueBg, FormulooBlue, Color(0xFF1565C0), "À valider")
        DocumentStatus.PENDING_CHAIN ->
            PillStyle(FormulooPurpleBg, FormulooPurple, Color(0xFF6A1B9A), "Blockchain…")
        DocumentStatus.TAMPERED ->
            PillStyle(FormulooErrorBg, FormulooError, FormulooError, "Alerte")
        DocumentStatus.FAILED ->
            PillStyle(FormulooErrorBg, FormulooError, FormulooError, "Échec")
        else ->
            PillStyle(FormulooSecondaryBg, FormulooSecondary, Color(0xFFE65100), "En cours")
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(style.bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(style.dotColor, CircleShape),
        )
        Text(
            style.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = style.textColor,
        )
    }
}

// ── Shared white card container ───────────────────────────────────────────────

@Composable
private fun DetailSurface(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
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

private fun formatDate(iso: String): String = try {
    val parts = iso.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (_: Exception) {
    iso
}

private fun formatAmount(value: Double, currency: String): String {
    val formatted = when {
        value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)} M"
        value >= 1_000 -> "${"%.0f".format(value / 1_000)} k"
        else -> "%.0f".format(value)
    }
    return "$formatted $currency"
}
