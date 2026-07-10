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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSecondary
import com.formuloo.core.designsystem.FormulooSecondaryBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.domain.model.LeaveStatus
import com.formuloo.feature.hr.presentation.viewmodel.MyLeavesViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLeavesScreen(
    onBack: () -> Unit,
    onRequestLeave: () -> Unit,
    viewModel: MyLeavesViewModel = koinViewModel(),
) {
    val state by viewModel.leavesState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = { Text("Mes congés", fontWeight = FontWeight.Bold) },
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
            FloatingActionButton(onClick = onRequestLeave, containerColor = FormulooPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle demande", tint = Color.White)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val tabs = listOf("En cours", "Historique")
            TabRow(selectedTabIndex = selectedTab, containerColor = FormulooSurface, contentColor = FormulooPrimary) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune demande de congé.", color = FormulooOnSurfaceVariant)
                }
                is UiState.Success -> {
                    val all = (state as UiState.Success<List<LeaveRequest>>).data
                    val filtered = all.filter { leave ->
                        if (selectedTab == 0) leave.status == LeaveStatus.PENDING else leave.status != LeaveStatus.PENDING
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Aucune demande dans cette catégorie.", color = FormulooOnSurfaceVariant)
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp)) {
                            items(filtered, key = { it.id }) { leave ->
                                LeaveRequestCard(leave = leave, onCancel = { viewModel.cancelLeave(leave.id) })
                                Spacer(Modifier.height(12.dp))
                            }
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
private fun LeaveRequestCard(leave: LeaveRequest, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(leave.leaveTypeLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
                StatusBadge(leave.status)
            }
            Spacer(Modifier.height(4.dp))
            Text("${leave.startDate} → ${leave.endDate} · ${leave.days}j", fontSize = 13.sp, color = FormulooOnSurfaceVariant)
            if (leave.isPendingSync) {
                Spacer(Modifier.height(6.dp))
                Text("🔄 En attente de synchronisation", fontSize = 12.sp, color = FormulooSecondary)
            }
            if (leave.status == LeaveStatus.PENDING) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancel) { Text("Annuler", color = FormulooError) }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: LeaveStatus) {
    val (bg, fg, label) = when (status) {
        LeaveStatus.PENDING -> Triple(FormulooSecondaryBg, FormulooSecondary, "En attente")
        LeaveStatus.APPROVED -> Triple(FormulooMint, FormulooPrimary, "Approuvé")
        LeaveStatus.REJECTED -> Triple(FormulooErrorBg, FormulooError, "Refusé")
        LeaveStatus.ANNULE -> Triple(FormulooOutline, FormulooOnSurfaceVariant, "Annulé")
    }
    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 11.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}
