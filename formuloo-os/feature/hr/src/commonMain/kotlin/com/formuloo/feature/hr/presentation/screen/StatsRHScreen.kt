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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.formuloo.core.designsystem.FormulooPurple
import com.formuloo.core.designsystem.FormulooPurpleBg
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.domain.model.StatsRH
import com.formuloo.feature.hr.presentation.viewmodel.StatsRHViewModel
import org.koin.compose.viewmodel.koinViewModel

private fun formatMontant(amount: Double): String = when {
    amount >= 1_000_000 -> {
        val m = kotlin.math.round(amount / 10_000.0) / 100
        "${m.toString().replace(".", ",")} M XAF"
    }
    amount >= 1_000 -> "${(amount / 1_000).toInt()} k XAF"
    else -> "${amount.toInt()} XAF"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsRHScreen(
    onBack: () -> Unit,
    viewModel: StatsRHViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tableau de bord RH", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FormulooTextPrimary)
                        Text("Statistiques en temps réel", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
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
        when (val s = state) {
            is UiState.Loading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FormulooPrimary)
            }
            is UiState.Error -> Box(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(s.message, color = FormulooError)
            }
            is UiState.Success -> StatsContent(
                stats = s.data,
                contentPadding = padding,
            )
            else -> {}
        }
    }
}

@Composable
private fun StatsContent(stats: StatsRH, contentPadding: androidx.compose.foundation.layout.PaddingValues) {
    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "Période : ${stats.periodeReference}",
                fontSize = 12.sp,
                color = FormulooOnSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        // ── Effectifs ─────────────────────────────────────────────────────
        item {
            SectionTitle("Effectifs")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.People,
                    iconBg = FormulooMint,
                    iconTint = FormulooPrimary,
                    value = "${stats.totalEmployees}",
                    label = "Total",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CheckCircle,
                    iconBg = FormulooMint,
                    iconTint = FormulooPrimary,
                    value = "${stats.activeEmployees}",
                    label = "Actifs",
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Warning,
                    iconBg = Color(0xFFFFF3CD),
                    iconTint = Color(0xFF856404),
                    value = "${stats.inactiveEmployees}",
                    label = "Inactifs",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.BeachAccess,
                    iconBg = FormulooSecondaryBg,
                    iconTint = FormulooSecondary,
                    value = "${stats.onLeaveEmployees}",
                    label = "En congé",
                )
            }
        }

        // ── Répartition par département ──────────────────────────────────
        if (stats.byDepartment.isNotEmpty()) {
            item {
                SectionTitle("Répartition par département")
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FormulooSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        stats.byDepartment.forEachIndexed { index, (dept, count) ->
                            if (index > 0) HorizontalDivider(color = FormulooOutline, thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(FormulooPrimary),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(dept, fontSize = 13.sp, color = FormulooTextPrimary)
                                }
                                Text(
                                    "$count pers.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FormulooPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Congés ────────────────────────────────────────────────────────
        item {
            SectionTitle("Congés (mois en cours)")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.BeachAccess,
                    iconBg = Color(0xFFFFF3CD),
                    iconTint = Color(0xFF856404),
                    value = "${stats.congesEnAttente}",
                    label = "En attente",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CheckCircle,
                    iconBg = FormulooMint,
                    iconTint = FormulooPrimary,
                    value = "${stats.congesApprouvesCeMois}",
                    label = "Approuvés",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Warning,
                    iconBg = Color(0xFFFFE8E8),
                    iconTint = FormulooError,
                    value = "${stats.congesRejetesCeMois}",
                    label = "Rejetés",
                )
            }
        }

        // ── Présences ─────────────────────────────────────────────────────
        item {
            SectionTitle("Présences (mois en cours)")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.HowToReg,
                    iconBg = FormulooPurpleBg,
                    iconTint = FormulooPurple,
                    value = "${stats.joursPresenceCeMois}",
                    label = "Jours pointés",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Group,
                    iconBg = FormulooSecondaryBg,
                    iconTint = FormulooSecondary,
                    value = "${stats.employesPresCeMois}",
                    label = "Employés présents",
                )
            }
        }

        // ── Paie (visible si RH_MANAGER) ─────────────────────────────────
        if (stats.masseSalarialeNette != null) {
            item {
                SectionTitle("Paie (mois en cours)")
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FormulooSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Masse salariale nette", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                                Text(
                                    formatMontant(stats.masseSalarialeNette),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = FormulooTextPrimary,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(FormulooMint)
                                    .padding(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = FormulooPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = FormulooOutline, thickness = 0.5.dp)
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            PayStatLine("Brouillons", stats.fichesBrouillon, Color(0xFF856404))
                            PayStatLine("Validées", stats.fichesValidees, FormulooPrimary)
                            PayStatLine("Payées", stats.fichesPayees, Color(0xFF10B981))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = FormulooOnSurfaceVariant,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    value: String,
    label: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = FormulooTextPrimary)
            Text(label, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
        }
    }
}

@Composable
private fun PayStatLine(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = FormulooOnSurfaceVariant)
    }
}
