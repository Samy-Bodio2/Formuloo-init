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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.InitialsAvatar
import com.formuloo.feature.hr.domain.model.AdminLeaveBalance
import com.formuloo.feature.hr.presentation.viewmodel.SoldesCongesAdminViewModel
import org.koin.compose.viewmodel.koinViewModel

private val avatarPalette = listOf(
    FormulooPrimary,
    Color(0xFF6366F1),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF3B82F6),
)

private fun avatarColorForId(id: String): Color =
    avatarPalette[kotlin.math.abs(id.hashCode()) % avatarPalette.size]

private fun congeTypeLabel(code: String): String = when (code) {
    "annuel" -> "Congé annuel"
    "maladie" -> "Maladie"
    "maternite" -> "Maternité"
    "paternite" -> "Paternité"
    "sans_solde" -> "Sans solde"
    "exceptionnel" -> "Exceptionnel"
    "recuperation" -> "Récupération"
    "formation" -> "Formation"
    "deces" -> "Décès"
    else -> code.replaceFirstChar { it.uppercaseChar() }
}

private fun formatJours(jours: Double): String {
    val i = jours.toInt()
    return if (jours == i.toDouble()) "$i j" else "${"%.1f".format(jours)} j"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoldesCongesAdminScreen(
    onBack: () -> Unit,
    viewModel: SoldesCongesAdminViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val annee by viewModel.annee.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Soldes de congés", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FormulooTextPrimary)
                        Text("Tous les employés", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = FormulooTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualiser", tint = FormulooTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // ── Sélecteur d'année ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.setAnnee(annee - 1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Année précédente", tint = FormulooTextPrimary)
                }
                Text(
                    "$annee",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FormulooTextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                IconButton(onClick = { viewModel.setAnnee(annee + 1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Année suivante", tint = FormulooTextPrimary)
                }
            }

            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun solde de congés pour $annee.", color = FormulooOnSurfaceVariant)
                }
                is UiState.Success -> {
                    val balances = s.data
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(
                                "${balances.size} SOLDE${if (balances.size > 1) "S" else ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FormulooOnSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        items(balances, key = { it.id }) { balance ->
                            LeaveBalanceCard(balance)
                        }
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = FormulooError)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun LeaveBalanceCard(balance: AdminLeaveBalance) {
    val progress = if (balance.joursAcquis > 0) {
        (balance.joursPris / balance.joursAcquis).toFloat().coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(
                    initials = balance.employeeInitials,
                    size = 38.dp,
                    backgroundColor = avatarColorForId(balance.employeeId),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(balance.employeeName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FormulooTextPrimary)
                    Text(congeTypeLabel(balance.typeConge), fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatJours(balance.joursRestants),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (balance.joursRestants <= 0) FormulooError else FormulooPrimary,
                    )
                    Text("restants", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
                }
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = if (progress >= 0.9f) FormulooError else FormulooPrimary,
                trackColor = FormulooMint,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SoldeLabel("Acquis", formatJours(balance.joursAcquis))
                SoldeLabel("Pris", formatJours(balance.joursPris))
                SoldeLabel("Restants", formatJours(balance.joursRestants))
            }
        }
    }
}

@Composable
private fun SoldeLabel(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FormulooTextPrimary)
        Text(label, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
    }
}
