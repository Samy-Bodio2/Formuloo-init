package com.formuloo.feature.hr.presentation.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.domain.model.Payslip
import com.formuloo.feature.hr.domain.model.PayslipStatus
import com.formuloo.feature.hr.presentation.viewmodel.PayrollAdminViewModel
import com.formuloo.feature.hr.presentation.viewmodel.PayrollRunState
import org.koin.compose.viewmodel.koinViewModel

private val MONTHS_FR_PAYROLL = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollAdminScreen(
    onBack: () -> Unit,
    viewModel: PayrollAdminViewModel = koinViewModel(),
) {
    val periodeState by viewModel.periodeState.collectAsStateWithLifecycle()
    val runState by viewModel.runState.collectAsStateWithLifecycle()
    val mois by viewModel.mois.collectAsStateWithLifecycle()
    val annee by viewModel.annee.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Génération de paie", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Administration RH", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooPrimary),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (!runState.isLoading) viewModel.runPayroll() },
                containerColor = FormulooPrimary,
                contentColor = Color.White,
            ) {
                if (runState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Générer la paie")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Sélecteur de mois ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FormulooSurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = viewModel::prevMonth) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Mois précédent", tint = FormulooPrimary)
                }
                Text(
                    "${MONTHS_FR_PAYROLL.getOrNull(mois - 1) ?: mois} $annee",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = FormulooTextPrimary,
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Mois suivant", tint = FormulooPrimary)
                }
            }
            HorizontalDivider(color = FormulooOutline)

            when (val s = periodeState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Aucun bulletin pour cette période.",
                                color = FormulooOnSurfaceVariant,
                                fontSize = 15.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Appuyez sur ▶ pour générer la paie.",
                                color = FormulooOnSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val payslips = s.data
                    val totalBrut = payslips.sumOf { it.gross }
                    val totalNet = payslips.sumOf { it.netSalary }

                    // ── Ligne de résumé ────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FormulooPrimary.copy(alpha = 0.06f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SummaryKpi("Bulletins", payslips.size.toString())
                        SummaryKpi("Brut total", "%,.0f XAF".format(totalBrut))
                        SummaryKpi("Net total", "%,.0f XAF".format(totalNet))
                    }
                    HorizontalDivider(color = FormulooOutline)

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(payslips, key = { it.id }) { payslip ->
                            PayrollPayslipCard(payslip)
                        }
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = FormulooError)
                }
            }
        }
    }

    // ── Résultat de la génération ──────────────────────────────────────────
    if (runState.result != null) {
        PayrollResultDialog(state = runState, onDismiss = viewModel::dismissRunResult)
    }
    if (runState.error != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRunResult,
            title = { Text("Erreur") },
            text = { Text(runState.error ?: "") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissRunResult) { Text("OK") }
            },
        )
    }
}

@Composable
private fun PayrollResultDialog(state: PayrollRunState, onDismiss: () -> Unit) {
    val r = state.result ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paie générée") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(r.message, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                ResultRow("Employés traités", r.nbEmployes.toString())
                ResultRow("Bulletins créés", r.nbCrees.toString(), Color(0xFF2E7D32))
                ResultRow("Déjà existants", r.nbIgnores.toString(), FormulooOnSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

@Composable
private fun ResultRow(label: String, value: String, valueColor: Color = FormulooTextPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun SummaryKpi(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooPrimary)
        Text(label, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
    }
}

@Composable
private fun PayrollPayslipCard(payslip: Payslip) {
    val (statusColor, statusLabel) = when (payslip.status) {
        PayslipStatus.BROUILLON -> FormulooOnSurfaceVariant to "Brouillon"
        PayslipStatus.VALIDE -> FormulooSecondary to "Validé"
        PayslipStatus.PAYE -> Color(0xFF2E7D32) to "Payé"
    }
    val initials = payslip.employeeName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(FormulooPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FormulooPrimary)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    payslip.employeeName.ifBlank { "Employé inconnu" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = FormulooTextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = statusColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Brut : %,.0f".format(payslip.gross),
                        fontSize = 12.sp,
                        color = FormulooOnSurfaceVariant,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%,.0f".format(payslip.netSalary),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FormulooTextPrimary,
                )
                Text(payslip.currency, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
            }
        }
    }
}
