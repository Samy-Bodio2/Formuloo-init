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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.domain.model.Presence
import com.formuloo.feature.hr.domain.model.PresenceStatus
import com.formuloo.feature.hr.presentation.viewmodel.MyPresencesViewModel
import com.formuloo.feature.hr.presentation.viewmodel.PresenceSummary
import org.koin.compose.viewmodel.koinViewModel

private val MONTHS_FR = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPresencesScreen(
    onBack: () -> Unit,
    viewModel: MyPresencesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val mois by viewModel.mois.collectAsStateWithLifecycle()
    val annee by viewModel.annee.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mes présences", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Pointage mensuel", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
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
                    "${MONTHS_FR.getOrNull(mois - 1) ?: mois} $annee",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = FormulooTextPrimary,
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Mois suivant", tint = FormulooPrimary)
                }
            }
            HorizontalDivider(color = FormulooOutline)

            when (val s = state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> {
                    SummaryRow(summary)
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Aucune présence enregistrée pour ce mois.", color = FormulooOnSurfaceVariant)
                    }
                }
                is UiState.Success -> {
                    SummaryRow(summary)
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                        items(s.data, key = { it.id }) { presence ->
                            PresenceCard(presence)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = FormulooError)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(summary: PresenceSummary) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SummaryChip("Présents", summary.present.toString(), Color(0xFF2E7D32)) }
        item { SummaryChip("Absents", summary.absent.toString(), Color(0xFFC62828)) }
        item { SummaryChip("Retards", summary.retard.toString(), Color(0xFFE65100)) }
        item { SummaryChip("Congés", summary.conge.toString(), Color(0xFF1565C0)) }
        if (summary.heuresTravaillees > 0) {
            item { SummaryChip("Heures", "%.1f h".format(summary.heuresTravaillees), Color(0xFF37474F)) }
        }
        if (summary.heuresSupplementaires > 0) {
            item { SummaryChip("Supp.", "+%.1f h".format(summary.heuresSupplementaires), Color(0xFF6A1B9A)) }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PresenceCard(presence: Presence) {
    val (statusColor, statusLabel) = presenceStatusMeta(presence.statut)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Date badge
            Column(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FormulooPrimary.copy(alpha = 0.08f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val parts = presence.date.split("-")
                Text(
                    parts.getOrNull(2) ?: "—",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = FormulooPrimary,
                )
                Text(
                    shortMonth(parts.getOrNull(1)?.toIntOrNull() ?: 0),
                    fontSize = 11.sp,
                    color = FormulooPrimary.copy(alpha = 0.75f),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                    if ((presence.heuresSupplementaires.toDoubleOrNull() ?: 0.0) > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "+${presence.heuresSupplementaires} h supp.",
                            fontSize = 11.sp,
                            color = Color(0xFF6A1B9A),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (presence.heureArrivee != null || presence.heureDepart != null) {
                    Text(
                        "${presence.heureArrivee ?: "—"} → ${presence.heureDepart ?: "—"}",
                        fontSize = 13.sp,
                        color = FormulooTextPrimary,
                    )
                }
                if (presence.heuresTravaillees != null) {
                    Text(
                        "${presence.heuresTravaillees} h travaillées",
                        fontSize = 12.sp,
                        color = FormulooOnSurfaceVariant,
                    )
                }
                if (!presence.commentaire.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(presence.commentaire, fontSize = 12.sp, color = FormulooOnSurfaceVariant)
                }
            }
        }
    }
}

private fun presenceStatusMeta(status: PresenceStatus): Pair<Color, String> = when (status) {
    PresenceStatus.PRESENT -> Color(0xFF2E7D32) to "Présent"
    PresenceStatus.ABSENT -> Color(0xFFC62828) to "Absent"
    PresenceStatus.RETARD -> Color(0xFFE65100) to "Retard"
    PresenceStatus.CONGE -> Color(0xFF1565C0) to "Congé"
    PresenceStatus.FERIE -> Color(0xFF6A1B9A) to "Férié"
}

private fun shortMonth(m: Int): String = when (m) {
    1 -> "jan"; 2 -> "fév"; 3 -> "mar"; 4 -> "avr"; 5 -> "mai"; 6 -> "jun"
    7 -> "jul"; 8 -> "aoû"; 9 -> "sep"; 10 -> "oct"; 11 -> "nov"; 12 -> "déc"
    else -> "—"
}
