package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.presentation.viewmodel.ContractCreateViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val contractTypes = listOf(
    "CDI" to "CDI",
    "CDD" to "CDD",
    "Stage" to "Stage",
    "Freelance" to "Freelance",
    "Intérim" to "Intérim",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContractCreateScreen(
    employeeId: String,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: ContractCreateViewModel = koinViewModel(parameters = { parametersOf(employeeId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onCreated()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nouveau contrat", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Formulaire de création", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
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
        containerColor = FormulooBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Section 1 — Type de contrat ───────────────────────────
                ContractSectionHeader(
                    number = 1,
                    title = "Type de contrat",
                    subtitle = "Sélectionnez la nature du lien contractuel",
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FormulooSurface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            contractTypes.forEach { (code, label) ->
                                val selected = state.contractType == code
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.onContractTypeChange(code) },
                                    label = {
                                        Text(
                                            label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FormulooPrimary,
                                        selectedLabelColor = Color.White,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = FormulooOutline,
                                        selectedBorderColor = FormulooPrimary,
                                    ),
                                )
                            }
                        }
                        if (state.contractType in listOf("CDD", "Stage")) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Un ${state.contractType} requiert une date de fin.",
                                fontSize = 12.sp,
                                color = Color(0xFFF57C00),
                            )
                        }
                    }
                }

                // ── Section 2 — Durée ─────────────────────────────────────
                ContractSectionHeader(
                    number = 2,
                    title = "Durée du contrat",
                    subtitle = "Dates de début et de fin",
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FormulooSurface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContractField(
                            label = "Date de début",
                            required = true,
                            errorMessage = state.errors["startDate"],
                        ) {
                            OutlinedTextField(
                                value = state.startDate,
                                onValueChange = viewModel::onStartDateChange,
                                placeholder = { Text("AAAA-MM-JJ", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.errors.containsKey("startDate"),
                                singleLine = true,
                                colors = contractFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                        val endRequired = state.contractType in listOf("CDD", "Stage")
                        ContractField(
                            label = "Date de fin",
                            required = endRequired,
                            errorMessage = state.errors["endDate"],
                        ) {
                            OutlinedTextField(
                                value = state.endDate,
                                onValueChange = viewModel::onEndDateChange,
                                placeholder = { Text("AAAA-MM-JJ", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.errors.containsKey("endDate"),
                                singleLine = true,
                                colors = contractFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    }
                }

                // ── Section 3 — Rémunération ──────────────────────────────
                ContractSectionHeader(
                    number = 3,
                    title = "Rémunération",
                    subtitle = "SMIG Cameroun : 36 270 XAF/mois minimum",
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FormulooSurface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContractField(
                            label = "Salaire brut (XAF)",
                            required = true,
                            errorMessage = state.errors["grossSalary"],
                        ) {
                            OutlinedTextField(
                                value = state.grossSalary,
                                onValueChange = viewModel::onGrossSalaryChange,
                                placeholder = { Text("ex : 150 000", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.errors.containsKey("grossSalary"),
                                singleLine = true,
                                colors = contractFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                suffix = { Text("XAF") },
                            )
                        }
                        ContractField(
                            label = "Heures / semaine",
                            required = false,
                        ) {
                            OutlinedTextField(
                                value = state.workHoursPerWeek,
                                onValueChange = viewModel::onWorkHoursChange,
                                placeholder = { Text("40", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = contractFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                suffix = { Text("h") },
                            )
                        }
                    }
                }

                // ── Section 4 — Options ───────────────────────────────────
                ContractSectionHeader(
                    number = 4,
                    title = "Options",
                    subtitle = "Période d'essai et document",
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FormulooSurface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContractField(
                            label = "Période d'essai",
                            required = false,
                        ) {
                            OutlinedTextField(
                                value = state.trialPeriod,
                                onValueChange = viewModel::onTrialPeriodChange,
                                placeholder = { Text("ex : 90", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = contractFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                suffix = { Text("jours") },
                            )
                        }
                        ContractField(
                            label = "URL document",
                            required = false,
                        ) {
                            OutlinedTextField(
                                value = state.documentUrl,
                                onValueChange = viewModel::onDocumentUrlChange,
                                placeholder = { Text("https://...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = contractFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            )
                        }
                    }
                }

                // Error banner
                if (state.errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, Color(0xFFE57373)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            state.errorMessage!!,
                            color = Color(0xFFB71C1C),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // ── Footer fixe ───────────────────────────────────────────────
            Surface(
                shadowElevation = 8.dp,
                color = FormulooSurface,
            ) {
                Button(
                    onClick = viewModel::submitForm,
                    enabled = !state.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary, contentColor = Color.White),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Création en cours…", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Filled.AddCard, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Créer le contrat", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ── Private helpers ─────────────────────────────────────────────────────────

@Composable
private fun ContractSectionHeader(number: Int, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(FormulooPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = FormulooTextPrimary)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ContractField(
    label: String,
    required: Boolean,
    errorMessage: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = FormulooTextPrimary)
            if (required) {
                Text(" *", color = Color.Red, fontSize = 13.sp)
            }
        }
        content()
        if (errorMessage != null) {
            Text(errorMessage, color = Color(0xFFD32F2F), fontSize = 11.sp)
        }
    }
}

@Composable
private fun contractFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FormulooPrimary,
    unfocusedBorderColor = FormulooOutline,
    focusedLabelColor = FormulooPrimary,
    cursorColor = FormulooPrimary,
)
