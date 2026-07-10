package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.formuloo.feature.hr.domain.model.Payslip
import com.formuloo.feature.hr.domain.model.PayslipStatus
import com.formuloo.feature.hr.presentation.viewmodel.PayslipListViewModel
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipListScreen(
    onBack: () -> Unit,
    onPayslipClick: (String) -> Unit,
    viewModel: PayslipListViewModel = koinViewModel(),
) {
    val state by viewModel.payslipsState.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val years = remember(selectedYear) { (selectedYear - 3..selectedYear).toList() }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Mes fiches de paie", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(years) { year ->
                    val selected = year == selectedYear
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.selectYear(year) }
                            .background(
                                if (selected) FormulooPrimary else FormulooSurface,
                                shape = RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            year.toString(),
                            color = if (selected) Color.White else FormulooTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune fiche de paie pour $selectedYear.", color = FormulooOnSurfaceVariant)
                }
                is UiState.Success -> {
                    val payslips = (state as UiState.Success<List<Payslip>>).data
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(payslips, key = { it.id }) { payslip ->
                            PayslipCard(payslip = payslip, onClick = { onPayslipClick(payslip.id) })
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
private fun PayslipCard(payslip: Payslip, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(payslip.period, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
                PayslipStatusBadge(payslip.status)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${formatAmount(payslip.netSalary)} ${payslip.currency}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = FormulooPrimary,
                )
                if (payslip.pdfUrl != null) {
                    Icon(Icons.Filled.Receipt, contentDescription = "PDF disponible", tint = FormulooSecondary)
                }
            }
        }
    }
}

@Composable
private fun PayslipStatusBadge(status: PayslipStatus) {
    val (bg, fg, label) = when (status) {
        PayslipStatus.BROUILLON -> Triple(FormulooOutline, FormulooOnSurfaceVariant, "Brouillon")
        PayslipStatus.VALIDE -> Triple(FormulooSecondaryBg, FormulooSecondary, "Validée")
        PayslipStatus.PAYE -> Triple(FormulooMint, FormulooPrimary, "Payée")
    }
    Box(
        modifier = Modifier.background(bg, shape = RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 11.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatAmount(value: Double): String {
    val rounded = value.toLong()
    val s = rounded.toString()
    val sb = StringBuilder()
    for ((index, char) in s.reversed().withIndex()) {
        if (index != 0 && index % 3 == 0) sb.append(' ')
        sb.append(char)
    }
    return sb.reverse().toString()
}
