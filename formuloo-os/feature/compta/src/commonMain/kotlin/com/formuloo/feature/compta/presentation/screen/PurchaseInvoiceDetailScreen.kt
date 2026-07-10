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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.compta.domain.model.PaymentMode
import com.formuloo.feature.compta.domain.model.PurchaseInvoice
import com.formuloo.feature.compta.domain.model.PurchaseInvoiceStatus
import com.formuloo.feature.compta.presentation.viewmodel.PurchaseInvoiceDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseInvoiceDetailScreen(
    purchaseInvoiceId: Int,
    onBack: () -> Unit,
    viewModel: PurchaseInvoiceDetailViewModel = koinViewModel(parameters = { parametersOf(purchaseInvoiceId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actionInProgress by viewModel.actionInProgress.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var showPayDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Détail achat", fontWeight = FontWeight.Bold) },
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
                    val achat = (state as UiState.Success<PurchaseInvoice>).data
                    PurchaseInvoiceDetailContent(
                        achat = achat,
                        actionInProgress = actionInProgress,
                        actionError = actionError,
                        onRecevoir = viewModel::recevoir,
                        onValider = viewModel::valider,
                        onPayer = { showPayDialog = true },
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

    if (showPayDialog) {
        PayDialog(
            onDismiss = { showPayDialog = false },
            onConfirm = { montant, mode, date, reference ->
                viewModel.payer(montant, mode, date, reference)
                showPayDialog = false
            },
        )
    }
}

@Composable
private fun PurchaseInvoiceDetailContent(
    achat: PurchaseInvoice,
    actionInProgress: Boolean,
    actionError: String?,
    onRecevoir: () -> Unit,
    onValider: () -> Unit,
    onPayer: () -> Unit,
    onEmettreAvoir: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(achat.numeroInterne, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FormulooTextPrimary)
                Text(achat.fournisseurNom, fontSize = 14.sp, color = FormulooOnSurfaceVariant)
            }
            PurchaseInvoiceStatusBadge(achat.statut)
        }
        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        achat.lignes.forEach { ligne ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${ligne.description} (x${ligne.quantite})", color = FormulooTextPrimary)
                Text("${formatAmount(ligne.montantTotal)} ${achat.devise}", color = FormulooTextPrimary)
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total TTC", color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Bold)
            Text("${formatAmount(achat.montantTtc)} ${achat.devise}", color = FormulooTextPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text("Date facture : ${achat.dateFacture}", fontSize = 13.sp, color = FormulooOnSurfaceVariant)
        Text("Échéance : ${achat.dateEcheance}", fontSize = 13.sp, color = FormulooOnSurfaceVariant)

        if (actionError != null) {
            Spacer(Modifier.height(12.dp))
            Text(actionError, color = FormulooError)
        }

        Spacer(Modifier.height(24.dp))
        when (achat.statut) {
            PurchaseInvoiceStatus.BROUILLON -> Button(
                onClick = onRecevoir,
                enabled = !actionInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Marquer comme reçue") }

            PurchaseInvoiceStatus.RECUE -> Button(
                onClick = onValider,
                enabled = !actionInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Valider (génère l'écriture comptable)") }

            PurchaseInvoiceStatus.VALIDEE, PurchaseInvoiceStatus.PARTIELLEMENT_PAYEE -> Button(
                onClick = onPayer,
                enabled = !actionInProgress,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer un paiement") }

            PurchaseInvoiceStatus.PAYEE, PurchaseInvoiceStatus.ANNULEE -> Unit
        }
        if (achat.canEmetAvoir) {
            Spacer(Modifier.height(8.dp))
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
private fun PayDialog(
    onDismiss: () -> Unit,
    onConfirm: (montant: Double, mode: PaymentMode, date: String, reference: String?) -> Unit,
) {
    var montant by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(PaymentMode.VIREMENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer un paiement") },
        text = {
            Column {
                OutlinedTextField(
                    value = montant,
                    onValueChange = { montant = it },
                    label = { Text("Montant") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (AAAA-MM-JJ)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Référence (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMode.entries.forEach { m ->
                        OutlinedButton(onClick = { mode = m }) {
                            Text(m.name, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(montant.toDoubleOrNull() ?: 0.0, mode, date, reference.ifBlank { null })
                },
                enabled = montant.toDoubleOrNull() != null && date.isNotBlank(),
            ) { Text("Confirmer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
