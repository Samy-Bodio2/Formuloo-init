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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
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
import com.formuloo.feature.compta.domain.model.PaymentMode
import com.formuloo.feature.compta.domain.model.SupplierPayment
import com.formuloo.feature.compta.presentation.viewmodel.SupplierPaymentListViewModel
import org.koin.compose.viewmodel.koinViewModel

private val MODE_LABELS = mapOf(
    PaymentMode.VIREMENT to "Virement",
    PaymentMode.CHEQUE to "Chèque",
    PaymentMode.ESPECES to "Espèces",
    PaymentMode.MOBILE_MONEY to "Mobile Money",
)

private val MODE_COLORS = mapOf(
    PaymentMode.VIREMENT to Color(0xFF1565C0),
    PaymentMode.CHEQUE to Color(0xFF6A1B9A),
    PaymentMode.ESPECES to Color(0xFF2E7D32),
    PaymentMode.MOBILE_MONEY to Color(0xFFE65100),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierPaymentListScreen(
    onBack: () -> Unit,
    viewModel: SupplierPaymentListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Paiements fournisseurs", fontWeight = FontWeight.Bold)
                        Text("Décaissements", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rafraîchir", tint = Color.White)
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
                is UiState.Empty -> EmptySupplierPayments()
                is UiState.Success -> {
                    val payments = (state as UiState.Success<List<SupplierPayment>>).data
                    SupplierPaymentList(payments)
                }
                is UiState.Error -> ErrorSupplierPayments(
                    message = (state as UiState.Error).message,
                    onRetry = viewModel::load,
                )
            }
        }
    }
}

@Composable
private fun SupplierPaymentList(payments: List<SupplierPayment>) {
    val totalDecaisse = payments.sumOf { it.montant }
    val devise = payments.firstOrNull()?.devise ?: "XAF"

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SummaryHeader(totalDecaisse, devise, payments.size)
        }
        items(payments, key = { it.id }) { payment ->
            SupplierPaymentCard(payment)
        }
    }
}

@Composable
private fun SummaryHeader(total: Double, devise: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FormulooPrimary),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Total décaissé", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatAmount(total)} $devise",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$count paiement${if (count > 1) "s" else ""}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun SupplierPaymentCard(payment: SupplierPayment) {
    val modeColor = MODE_COLORS[payment.modePaiement] ?: FormulooPrimary
    val modeLabel = MODE_LABELS[payment.modePaiement] ?: payment.modePaiement.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(FormulooSecondaryBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null, tint = FormulooSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            payment.fournisseurNom ?: "Fournisseur inconnu",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = FormulooTextPrimary,
                        )
                        Text(
                            payment.factureNumero ?: "Facture #${payment.factureFournisseurId}",
                            fontSize = 11.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }
                Text(
                    "${formatAmount(payment.montant)} ${payment.devise}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FormulooError,
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = FormulooOutline.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(modeColor.copy(alpha = 0.1f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(modeLabel, fontSize = 11.sp, color = modeColor, fontWeight = FontWeight.Medium)
                    }
                    payment.reference?.let { ref ->
                        Text("Réf. $ref", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
                    }
                }
                Text(payment.datePaiement, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptySupplierPayments() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(FormulooMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Payments, contentDescription = null, tint = FormulooPrimary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Aucun paiement fournisseur", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = FormulooTextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "Les paiements apparaîtront ici une fois les factures fournisseurs réglées.",
                fontSize = 13.sp,
                color = FormulooOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorSupplierPayments(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = FormulooError, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Réessayer", color = FormulooPrimary)
            }
        }
    }
}
