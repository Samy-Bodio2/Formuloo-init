package com.formuloo.feature.gesdoc.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTeal
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.gesdoc.domain.model.DocumentFullDetail
import com.formuloo.feature.gesdoc.domain.model.DocumentStatus
import com.formuloo.feature.gesdoc.presentation.viewmodel.DocumentOriginalViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentOriginalScreen(
    documentId: String,
    onBack: () -> Unit,
    viewModel: DocumentOriginalViewModel = koinViewModel(parameters = { parametersOf(documentId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val detail = state.detail

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Document original",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White,
                        )
                        val subtitle = listOfNotNull(
                            detail?.documentNumber,
                            detail?.supplier,
                        ).joinToString(" · ")
                        if (subtitle.isNotEmpty()) {
                            Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { detail?.viewableUrl?.let(uriHandler::openUri) }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Partager", tint = Color.White)
                    }
                    IconButton(onClick = { detail?.viewableUrl?.let(uriHandler::openUri) }) {
                        Icon(Icons.Filled.Print, contentDescription = "Imprimer", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooTextPrimary),
            )
        },
        bottomBar = {
            if (detail?.viewableUrl != null) {
                OriginalDocumentFooter(onOpen = { uriHandler.openUri(detail.viewableUrl!!) })
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize().padding(top = 64.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                state.error != null -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FormulooErrorBg, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = FormulooError, modifier = Modifier.size(16.dp))
                    Text(state.error!!, fontSize = 13.sp, color = FormulooError)
                }
                detail != null -> DocumentReconstructionCard(detail)
            }
        }
    }
}

// ── Carte de reconstruction du document ──────────────────────────────────────

@Composable
private fun DocumentReconstructionCard(detail: DocumentFullDetail) {
    val amountHt = detail.amountHt
    val tvaRate = detail.tvaRate
    val amountTtc = detail.amountTtc
    val tvaAmount = if (amountHt != null && tvaRate != null) amountHt * tvaRate / 100 else null
    val isCertified = detail.status == DocumentStatus.CERTIFIED

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    (detail.supplier ?: "Fournisseur").uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = FormulooTeal,
                    modifier = if (isCertified) Modifier.padding(end = 92.dp) else Modifier,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Facture N° ${detail.documentNumber ?: "—"} · ${detail.invoiceDate ?: "—"}",
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(FormulooTeal),
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            detail.supplier ?: "Fournisseur",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FormulooTextPrimary,
                        )
                        Text(
                            "Fournisseur · ${detail.documentType.label}",
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                    StatusPill(status = detail.status)
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "DÉTAIL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FormulooOnSurfaceVariant,
                    letterSpacing = 0.8.sp,
                )
                Spacer(Modifier.height(8.dp))

                if (amountHt != null) {
                    OriginalDetailRow("Montant HT", formatAmount(amountHt))
                }
                if (tvaAmount != null && tvaRate != null) {
                    OriginalDetailRow("TVA ${formatRate(tvaRate)} %", formatAmount(tvaAmount))
                }
                if (amountTtc != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(FormulooMint)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Net à payer TTC", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooTextPrimary)
                        Text(
                            "${formatAmount(amountTtc)} ${detail.currency ?: ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FormulooTextPrimary,
                        )
                    }
                }

                if (isCertified && detail.hashSha256 != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Document certifié — hash SHA-256 ancré sur Ethereum\n" +
                            "Empreinte ${detail.hashSha256.take(18)}…\n" +
                            "Toute modification postérieure serait détectée",
                        fontSize = 11.sp,
                        color = FormulooOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (isCertified) {
                CertifiedStamp(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
private fun OriginalDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
        Text(value, fontSize = 13.sp, color = FormulooTextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CertifiedStamp(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 18.dp, y = 22.dp)
            .rotate(28f)
            .background(FormulooMint)
            .padding(horizontal = 22.dp, vertical = 4.dp),
    ) {
        Text(
            "CERTIFIÉ BLOCKCHAIN",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = FormulooTeal,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun StatusPill(status: DocumentStatus) {
    val (bg, fg) = when (status) {
        DocumentStatus.CERTIFIED -> FormulooMint to FormulooTeal
        DocumentStatus.TAMPERED, DocumentStatus.FAILED -> FormulooErrorBg to FormulooError
        else -> FormulooOutline to FormulooOnSurfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(status.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

// ── Footer fixe : imprimer + télécharger ──────────────────────────────────────

@Composable
private fun OriginalDocumentFooter(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FormulooTextPrimary),
        ) {
            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Imprimer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Button(
            onClick = onOpen,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FormulooTeal),
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Télécharger", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatAmount(value: Double): String {
    val rounded = value.toLong()
    val sb = StringBuilder()
    rounded.toString().reversed().forEachIndexed { i, c ->
        if (i > 0 && i % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return sb.toString().reversed()
}

private fun formatRate(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
