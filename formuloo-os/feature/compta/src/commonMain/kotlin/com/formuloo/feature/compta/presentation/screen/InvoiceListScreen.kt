package com.formuloo.feature.compta.presentation.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.compta.domain.model.Invoice
import com.formuloo.feature.compta.domain.model.InvoiceStatus
import com.formuloo.feature.compta.presentation.viewmodel.InvoiceListViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    onBack: () -> Unit,
    onInvoiceClick: (Int) -> Unit,
    onCreateInvoice: () -> Unit,
    viewModel: InvoiceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Factures clients", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateInvoice, containerColor = FormulooSecondary) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle facture", tint = Color.White)
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune facture pour le moment.", color = FormulooOnSurfaceVariant)
                }
                is UiState.Success -> {
                    val invoices = (state as UiState.Success<List<Invoice>>).data
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(invoices, key = { it.id }) { invoice ->
                            InvoiceCard(invoice = invoice, onClick = { onInvoiceClick(invoice.id) })
                        }
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text((state as UiState.Error).message, color = FormulooError)
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(invoice.numero, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
                    Text(invoice.clientNom, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
                }
                InvoiceStatusBadge(invoice.statut)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${formatAmount(invoice.montantTtc)} ${invoice.devise}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = FormulooPrimary,
                )
                Text("Échéance ${invoice.dateEcheance}", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun InvoiceStatusBadge(status: InvoiceStatus) {
    val (bg, fg, label) = when (status) {
        InvoiceStatus.BROUILLON -> Triple(FormulooOutline, FormulooOnSurfaceVariant, "Brouillon")
        InvoiceStatus.EMISE -> Triple(FormulooSecondaryBg, FormulooSecondary, "Émise")
        InvoiceStatus.PARTIELLEMENT_PAYEE -> Triple(FormulooSecondaryBg, FormulooSecondary, "Partiellement payée")
        InvoiceStatus.PAYEE -> Triple(FormulooMint, FormulooPrimary, "Payée")
        InvoiceStatus.ANNULEE -> Triple(FormulooOutline, FormulooError, "Annulée")
    }
    Box(
        modifier = Modifier.background(bg, shape = RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 11.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}
