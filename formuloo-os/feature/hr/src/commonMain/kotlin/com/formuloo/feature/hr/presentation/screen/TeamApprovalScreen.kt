package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.common.model.PendingRequest
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.PendingRequestItem
import com.formuloo.feature.hr.domain.model.LeaveRequest
import com.formuloo.feature.hr.presentation.viewmodel.TeamApprovalViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamApprovalScreen(
    onBack: () -> Unit,
    viewModel: TeamApprovalViewModel = koinViewModel(),
) {
    val state by viewModel.pendingState.collectAsStateWithLifecycle()
    val conflictMessage by viewModel.conflictMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var rejectTargetId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    LaunchedEffect(conflictMessage) {
        conflictMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.dismissConflictMessage()
        }
    }

    rejectTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { rejectTargetId = null; rejectReason = "" },
            title = { Text("Motif de refus") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    placeholder = { Text("Indiquez le motif du refus…") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FormulooPrimary,
                        unfocusedBorderColor = FormulooOutline,
                    ),
                    minLines = 2,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rejectReason.isNotBlank()) {
                            viewModel.reject(id, rejectReason)
                            rejectTargetId = null
                            rejectReason = ""
                        }
                    },
                ) { Text("Refuser", color = FormulooError) }
            },
            dismissButton = {
                TextButton(onClick = { rejectTargetId = null; rejectReason = "" }) { Text("Annuler") }
            },
        )
    }

    Scaffold(
        containerColor = FormulooBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Approbations équipe", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            when (state) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
                is UiState.Empty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 48.sp)
                        Text("Aucune demande en attente", color = FormulooOnSurfaceVariant)
                    }
                }
                is UiState.Success -> {
                    val leaves = (state as UiState.Success<List<LeaveRequest>>).data
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(leaves, key = { it.id }) { leave ->
                            PendingRequestItem(
                                request = PendingRequest(
                                    id = leave.id,
                                    employeeName = leave.employeeName,
                                    initials = leave.employeeInitials,
                                    type = leave.leaveTypeLabel,
                                    duration = "${leave.days}j",
                                ),
                                onApprove = { viewModel.approve(leave.id) },
                                onReject = { rejectTargetId = leave.id },
                            )
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
