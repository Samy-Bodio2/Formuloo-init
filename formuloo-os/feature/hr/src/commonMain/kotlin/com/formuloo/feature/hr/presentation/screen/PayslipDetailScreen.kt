package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.domain.model.Payslip
import com.formuloo.feature.hr.domain.model.PayslipStatus
import com.formuloo.feature.hr.util.PdfOpener
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val PayslipDarkBg = Color(0xFF1A1D23)
private val PayslipDarkCard = Color(0xFF252830)

@Composable
fun PayslipDetailScreen(
    payslipId: String,
    onBack: () -> Unit,
    employeeName: String = "",
    employeeNumber: String = "",
    employeePosition: String = "",
    repository: HrRepository = koinInject(),
    pdfOpener: PdfOpener = koinInject(),
) {
    var payslip by remember { mutableStateOf<Payslip?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isPdfLoading by remember { mutableStateOf(false) }
    var pdfError by remember { mutableStateOf<String?>(null) }
    var isActionLoading by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var showPayDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(payslipId) {
        when (val result = repository.getPayslipDetail(payslipId)) {
            is NetworkResult.Success -> payslip = result.data
            is NetworkResult.Error -> error = result.message
            else -> {}
        }
    }

    val downloadPdf: () -> Unit = {
        scope.launch {
            isPdfLoading = true
            pdfError = null
            val filename = "bulletin_paie_${payslipId}.pdf"
            when (val result = repository.downloadPayslipPdf(payslipId)) {
                is NetworkResult.Success -> {
                    try {
                        pdfOpener.openPdf(result.data, filename)
                    } catch (e: Exception) {
                        pdfError = "Impossible d'ouvrir le PDF. Installez un lecteur PDF."
                    }
                }
                is NetworkResult.Error -> pdfError = result.message
                else -> {}
            }
            isPdfLoading = false
        }
    }

    val onValidate: () -> Unit = {
        scope.launch {
            isActionLoading = true
            actionError = null
            when (val result = repository.validatePayslip(payslipId)) {
                is NetworkResult.Success -> payslip = result.data
                is NetworkResult.Error -> actionError = result.message
                else -> {}
            }
            isActionLoading = false
        }
    }

    val onPay: (String) -> Unit = { mode ->
        scope.launch {
            isActionLoading = true
            actionError = null
            when (val result = repository.payPayslip(payslipId, mode)) {
                is NetworkResult.Success -> payslip = result.data
                is NetworkResult.Error -> actionError = result.message
                else -> {}
            }
            isActionLoading = false
        }
    }

    if (showPayDialog) {
        ModePaiementDialog(
            onConfirm = { mode ->
                showPayDialog = false
                onPay(mode)
            },
            onDismiss = { showPayDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PayslipDarkBg),
    ) {
        // ── Header sombre ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Bulletin de paie",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 17.sp,
                )
                val subtitle = buildList {
                    payslip?.period?.let { add(it) }
                    if (employeeName.isNotBlank()) add(employeeName)
                }.joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }
            IconButton(onClick = downloadPdf, enabled = !isPdfLoading) {
                Icon(Icons.Filled.Share, contentDescription = "Partager", tint = Color.White)
            }
            IconButton(onClick = downloadPdf, enabled = !isPdfLoading) {
                Icon(Icons.Filled.Print, contentDescription = "Imprimer", tint = Color.White)
            }
        }
        if (pdfError != null) {
            Text(
                pdfError!!,
                color = Color(0xFFFF6B6B),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                textAlign = TextAlign.Center,
            )
        }

        // ── Contenu principal ────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error.orEmpty(), color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                }
                payslip == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    PayslipCard(
                        payslip = payslip!!,
                        employeeName = employeeName,
                        employeeNumber = employeeNumber,
                        employeePosition = employeePosition,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ── Action workflow (Valider / Payer) ────────────────────────────
        val currentStatus = payslip?.status
        if (currentStatus == PayslipStatus.BROUILLON || currentStatus == PayslipStatus.VALIDE) {
            if (actionError != null) {
                Text(
                    actionError!!,
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                when (currentStatus) {
                    PayslipStatus.BROUILLON -> Button(
                        onClick = onValidate,
                        enabled = !isActionLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White,
                        ),
                    ) {
                        if (isActionLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Valider la fiche de paie", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    PayslipStatus.VALIDE -> Button(
                        onClick = { showPayDialog = true },
                        enabled = !isActionLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            contentColor = Color.White,
                        ),
                    ) {
                        if (isActionLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Marquer comme payé", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    PayslipStatus.PAYE -> {}
                }
            }
        }

        // ── Footer boutons ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = downloadPdf,
                enabled = !isPdfLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(25.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PayslipDarkCard,
                    contentColor = Color.White,
                ),
            ) {
                if (isPdfLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Print, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Imprimer", fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = downloadPdf,
                enabled = !isPdfLoading,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary, contentColor = Color.White),
            ) {
                if (isPdfLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Télécharger", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Carte blanche du bulletin ─────────────────────────────────────────────

@Composable
private fun PayslipCard(
    payslip: Payslip,
    employeeName: String,
    employeeNumber: String,
    employeePosition: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FormulooSurface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── En-tête période ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "BULLETIN DE PAIE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FormulooLabelGray,
                        letterSpacing = 0.6.sp,
                    )
                    Text(
                        payslip.period.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FormulooLabelGray,
                        letterSpacing = 0.6.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = FormulooPrimary.copy(alpha = 0.4f), thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // ── Employé ────────────────────────────────────────────────
            if (employeeName.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(employeeName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = FormulooTextPrimary)
                    PayslipDetailStatusBadge(payslip.status)
                }
                val subLine = listOfNotNull(
                    employeeNumber.ifBlank { null },
                    employeePosition.ifBlank { null },
                ).joinToString(" · ")
                if (subLine.isNotBlank()) {
                    Text(subLine, fontSize = 13.sp, color = FormulooLabelGray)
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Gains ─────────────────────────────────────────────────
            SectionLabel("GAINS")
            Spacer(Modifier.height(6.dp))
            PayslipAmountRow("Salaire de base", payslip.gross, payslip.currency, isDeduction = false)
            if (payslip.primeTransport > 0) PayslipAmountRow("Indemnité de transport", payslip.primeTransport, payslip.currency, false)
            if (payslip.primeLogement > 0) PayslipAmountRow("Prime de logement", payslip.primeLogement, payslip.currency, false)
            if (payslip.primeRendement > 0) PayslipAmountRow("Prime de rendement", payslip.primeRendement, payslip.currency, false)
            if (payslip.autresPrimes > 0) PayslipAmountRow("Autres primes", payslip.autresPrimes, payslip.currency, false)

            Spacer(Modifier.height(12.dp))

            // ── Retenues ───────────────────────────────────────────────
            SectionLabel("RETENUES")
            Spacer(Modifier.height(6.dp))
            if (payslip.cotisationCnps > 0) PayslipAmountRow("CNPS (4,2 %)", payslip.cotisationCnps, payslip.currency, true)
            if (payslip.impotIrpp > 0) PayslipAmountRow("Impôt sur le revenu (IRPP)", payslip.impotIrpp, payslip.currency, true)
            if (payslip.creditLogement > 0) PayslipAmountRow("Crédit logement", payslip.creditLogement, payslip.currency, true)
            if (payslip.autresDeductions > 0) PayslipAmountRow("Autres retenues", payslip.autresDeductions, payslip.currency, true)

            Spacer(Modifier.height(16.dp))

            // ── Net à payer ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FormulooMint)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Net à payer", fontSize = 14.sp, color = FormulooPrimary)
                Text(
                    "${formatBulletinAmount(payslip.netSalary)} ${payslip.currency}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = FormulooPrimary,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Texte légal ────────────────────────────────────────────
            val journal = "OD-PAIE-%04d-%02d".format(payslip.annee, payslip.mois)
            Text(
                "Document généré par Formuloo OS · SYSCOHADA\n" +
                    "Journal $journal · Conforme au Code du travail camerounais\n" +
                    "Ce bulletin est à conserver sans limitation de durée.",
                fontSize = 10.sp,
                color = FormulooOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = FormulooLabelGray,
        letterSpacing = 0.6.sp,
    )
}

@Composable
private fun PayslipAmountRow(label: String, amount: Double, currency: String, isDeduction: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = FormulooTextPrimary, modifier = Modifier.weight(1f))
        Text(
            "${if (isDeduction) "− " else ""}${formatBulletinAmount(amount)} $currency",
            fontSize = 14.sp,
            color = if (isDeduction) FormulooError else FormulooTextPrimary,
            fontWeight = if (isDeduction) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun PayslipDetailStatusBadge(status: PayslipStatus) {
    val (bg, fg, label) = when (status) {
        PayslipStatus.BROUILLON -> Triple(Color(0xFFF0F0F0), FormulooOnSurfaceVariant, "Brouillon")
        PayslipStatus.VALIDE -> Triple(Color(0xFFFFF3E0), Color(0xFFF57C00), "Validé")
        PayslipStatus.PAYE -> Triple(FormulooMint, FormulooPrimary, "• Payé")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 12.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModePaiementDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val modes = listOf(
        "virement" to "Virement bancaire",
        "especes" to "Espèces",
        "mobile_money" to "Mobile Money",
        "cheque" to "Chèque",
    )
    var selected by remember { mutableStateOf("virement") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mode de paiement", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Sélectionnez le mode de paiement utilisé pour cette fiche.",
                    fontSize = 14.sp,
                    color = FormulooOnSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                modes.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == code,
                            onClick = { selected = code },
                            colors = RadioButtonDefaults.colors(selectedColor = FormulooPrimary),
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
            ) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

private fun formatBulletinAmount(value: Double): String {
    val n = kotlin.math.abs(value).toLong()
    val s = n.toString()
    val sb = StringBuilder()
    for ((i, c) in s.reversed().withIndex()) {
        if (i != 0 && i % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return sb.reverse().toString()
}
