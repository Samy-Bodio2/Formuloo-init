package com.formuloo.feature.compta.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.compta.domain.model.Invoice
import com.formuloo.feature.compta.domain.model.InvoiceStatus
import com.formuloo.feature.compta.presentation.viewmodel.InvoiceDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Int,
    onBack: () -> Unit,
    viewModel: InvoiceDetailViewModel = koinViewModel(parameters = { parametersOf(invoiceId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actionInProgress by viewModel.actionInProgress.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Détail facture", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FormulooPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Success -> {
                    val invoice = (state as UiState.Success<Invoice>).data
                    InvoiceDetailContent(
                        invoice = invoice,
                        actionInProgress = actionInProgress,
                        onEmettre = viewModel::emettre,
                        onEmettreAvoir = viewModel::emettreAvoir,
                    )
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text((state as UiState.Error).message, color = FormulooError)
                }
                is UiState.Empty -> Unit
            }
        }
    }
}

@Composable
private fun InvoiceDetailContent(
    invoice: Invoice,
    actionInProgress: Boolean,
    onEmettre: () -> Unit,
    onEmettreAvoir: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(invoice.numero, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FormulooTextPrimary)
                Text(invoice.clientNom, fontSize = 14.sp, color = FormulooOnSurfaceVariant)
            }
            InvoiceStatusBadge(invoice.statut)
        }
        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        invoice.lignes.forEach { ligne ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${ligne.description} (x${ligne.quantite})", color = FormulooTextPrimary)
                Text("${formatAmount(ligne.montantTotal)} ${invoice.devise}", color = FormulooTextPrimary)
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        AmountRow("Montant HT", invoice.montantHt, invoice.devise)
        AmountRow("TVA (${invoice.tvaTaux}%)", invoice.tva, invoice.devise)
        Spacer(Modifier.height(8.dp))
        AmountRow("Total TTC", invoice.montantTtc, invoice.devise, bold = true)

        Spacer(Modifier.height(8.dp))
        Text("Échéance : ${invoice.dateEcheance}", fontSize = 13.sp, color = FormulooOnSurfaceVariant)
        invoice.dateEmission?.let { Text("Émise le : $it", fontSize = 13.sp, color = FormulooOnSurfaceVariant) }

        Spacer(Modifier.height(24.dp))
        if (invoice.statut == InvoiceStatus.BROUILLON) {
            Button(
                onClick = onEmettre,
                enabled = !actionInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (actionInProgress) "Émission en cours…" else "Émettre la facture")
            }
            Spacer(Modifier.height(8.dp))
        }
        if (invoice.canEmetAvoir) {
            OutlinedButton(
                onClick = onEmettreAvoir,
                enabled = !actionInProgress,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FormulooError),
            ) {
                Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(if (actionInProgress) "Création en cours…" else "Émettre un avoir")
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, amount: Double, devise: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = FormulooOnSurfaceVariant, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            "${formatAmount(amount)} $devise",
            color = FormulooTextPrimary,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
