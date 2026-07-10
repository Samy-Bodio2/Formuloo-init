package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChildFriendly
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooBlue
import com.formuloo.core.designsystem.FormulooBlueBg
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooPurple
import com.formuloo.core.designsystem.FormulooPurpleBg
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.data.mapper.toLibelleFr
import com.formuloo.feature.hr.domain.model.LeaveTypeCode
import com.formuloo.feature.hr.domain.util.DateUtils
import com.formuloo.feature.hr.presentation.viewmodel.LeaveFormState
import com.formuloo.feature.hr.presentation.viewmodel.LeaveRequestViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    employeeName: String = "",
    viewModel: LeaveRequestViewModel = koinViewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    var currentStep by remember { mutableIntStateOf(1) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    LaunchedEffect(submitState) {
        if (submitState is UiState.Success) currentStep = 4
    }

    if (showStartPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.updateStartDate(DateUtils.epochMillisToIso(it)) }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Annuler") } },
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.updateEndDate(DateUtils.epochMillisToIso(it)) }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Annuler") } },
        ) { DatePicker(state = state) }
    }

    Scaffold(containerColor = FormulooBackground) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LeaveStepHeader(
                employeeName = employeeName,
                currentStep = currentStep,
                onBack = {
                    if (currentStep == 1 || currentStep == 4) onBack()
                    else currentStep--
                },
            )

            HorizontalDivider(color = FormulooOutline, thickness = 0.5.dp)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val days = viewModel.calculateDays()
                when (currentStep) {
                    1 -> StepTypeConge(
                        selectedType = formState.type,
                        onTypeSelected = viewModel::updateType,
                    )
                    2 -> StepDates(
                        formState = formState,
                        days = days,
                        onStartDateClick = { showStartPicker = true },
                        onEndDateClick = { showEndPicker = true },
                    )
                    3 -> StepMotif(
                        reason = formState.reason,
                        onReasonChange = viewModel::updateReason,
                        submitError = (submitState as? UiState.Error)?.message,
                    )
                    4 -> ConfirmationContent(
                        formState = formState,
                        days = days,
                        employeeName = employeeName,
                    )
                }
            }

            HorizontalDivider(color = FormulooOutline, thickness = 0.5.dp)

            when (currentStep) {
                1 -> LeaveFooter(primaryText = "Continuer →", onPrimary = { currentStep = 2 })
                2 -> LeaveFooter(primaryText = "Continuer →", onPrimary = { currentStep = 3 }, onBack = { currentStep = 1 })
                3 -> LeaveFooter(
                    primaryText = "Soumettre la demande →",
                    onPrimary = viewModel::submit,
                    onBack = { currentStep = 2 },
                    primaryLoading = submitState is UiState.Loading,
                )
                4 -> LeaveFooter(primaryText = "Retour à la fiche →", onPrimary = onSubmitted)
            }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────────────────

@Composable
private fun LeaveStepHeader(
    employeeName: String,
    currentStep: Int,
    onBack: () -> Unit,
) {
    val totalSteps = 3
    val showProgress = currentStep in 1..totalSteps

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = FormulooTextPrimary)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (employeeName.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FormulooMint)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Person, null, tint = FormulooPrimary, modifier = Modifier.size(14.dp))
                        Text(employeeName, fontSize = 13.sp, color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.size(40.dp))
        }

        if (showProgress) {
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 1..totalSteps) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= currentStep) FormulooPrimary else FormulooOutline),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Étape $currentStep sur $totalSteps",
                fontSize = 11.sp,
                color = FormulooLabelGray,
                letterSpacing = 0.3.sp,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = when (currentStep) {
                1 -> "Type de congé"
                2 -> "Dates de l'absence"
                3 -> "Motif & justificatif"
                else -> "Demande envoyée ✓"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = FormulooTextPrimary,
        )
        Text(
            text = when (currentStep) {
                1 -> "Sélectionnez le type de congé souhaité"
                2 -> "Indiquez les dates de votre absence"
                3 -> "Ajoutez un motif et un justificatif si nécessaire"
                else -> "Votre demande a été transmise avec succès"
            },
            fontSize = 13.sp,
            color = FormulooOnSurfaceVariant,
        )
    }
}

// ── Step 1 — Type de congé ─────────────────────────────────────────────────

private data class LeaveTypeOption(
    val code: LeaveTypeCode,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBg: Color,
    val subtitle: String,
)

@Composable
private fun StepTypeConge(selectedType: LeaveTypeCode, onTypeSelected: (LeaveTypeCode) -> Unit) {
    val options = listOf(
        LeaveTypeOption(LeaveTypeCode.ANNUEL, Icons.Filled.DateRange, FormulooPrimary, FormulooMint, "Jusqu'à 30 jours par an"),
        LeaveTypeOption(LeaveTypeCode.MALADIE, Icons.Filled.MedicalServices, FormulooError, FormulooErrorBg, "Sur présentation d'un certificat"),
        LeaveTypeOption(LeaveTypeCode.SANS_SOLDE, Icons.Filled.HourglassEmpty, FormulooLabelGray, Color(0xFFF0F0F0), "Sans maintien du salaire"),
        LeaveTypeOption(LeaveTypeCode.MATERNITE, Icons.Filled.ChildFriendly, FormulooPurple, FormulooPurpleBg, "Congé maternité / paternité"),
        LeaveTypeOption(LeaveTypeCode.FORMATION, Icons.Filled.School, FormulooBlue, FormulooBlueBg, "Formation professionnelle"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { option ->
            LeaveTypeCard(
                option = option,
                isSelected = selectedType == option.code,
                onSelect = { onTypeSelected(option.code) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveTypeCard(
    option: LeaveTypeOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) FormulooMint else FormulooSurface),
        border = if (isSelected) BorderStroke(1.5.dp, FormulooPrimary) else BorderStroke(1.dp, FormulooOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)).background(option.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(option.icon, null, tint = option.iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(option.code.toLibelleFr(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = FormulooTextPrimary)
                Text(option.subtitle, fontSize = 12.sp, color = FormulooOnSurfaceVariant)
            }
            if (isSelected) {
                Box(
                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(50)).background(FormulooPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ── Step 2 — Dates ─────────────────────────────────────────────────────────

@Composable
private fun StepDates(
    formState: LeaveFormState,
    days: Int,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateFieldButton(
                label = "Du *",
                value = formState.startDate,
                isError = formState.errors.containsKey("startDate"),
                onClick = onStartDateClick,
                modifier = Modifier.weight(1f),
            )
            DateFieldButton(
                label = "Au *",
                value = formState.endDate,
                isError = formState.errors.containsKey("endDate"),
                onClick = onEndDateClick,
                modifier = Modifier.weight(1f),
            )
        }

        formState.errors["startDate"]?.let { Text(it, color = FormulooError, fontSize = 12.sp) }
        formState.errors["endDate"]?.let { Text(it, color = FormulooError, fontSize = 12.sp) }

        if (formState.startDate.isNotBlank() && formState.endDate.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FormulooMint)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.CalendarMonth, null, tint = FormulooPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FormulooPrimary)) {
                            append("$days")
                        }
                        withStyle(SpanStyle(fontSize = 15.sp, color = FormulooTextPrimary)) {
                            append(" jour${if (days > 1) "s" else ""} d'absence")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DateFieldButton(
    label: String,
    value: String,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (isError) FormulooError else FormulooLabelGray,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isError) FormulooError else FormulooOutline),
            contentPadding = PaddingValues(horizontal = 10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (value.isBlank()) FormulooLabelGray else FormulooTextPrimary,
            ),
        ) {
            Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = FormulooPrimary)
            Spacer(Modifier.width(6.dp))
            Text(
                if (value.isBlank()) "Sélectionner" else value,
                fontSize = 12.sp,
                fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.Medium,
            )
        }
    }
}

// ── Step 3 — Motif & justificatif ──────────────────────────────────────────

@Composable
private fun StepMotif(
    reason: String,
    onReasonChange: (String) -> Unit,
    submitError: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Motif",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = FormulooLabelGray,
            letterSpacing = 0.3.sp,
        )
        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            placeholder = {
                Text(
                    "Décrivez le motif de votre absence (optionnel)",
                    color = FormulooLabelGray,
                    fontSize = 14.sp,
                )
            },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FormulooPrimary,
                unfocusedBorderColor = FormulooOutline,
            ),
        )

        Text(
            "Justificatif",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = FormulooLabelGray,
            letterSpacing = 0.3.sp,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FormulooBackground)
                .drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 7f), 0f)
                    drawRoundRect(
                        color = FormulooOutline,
                        style = Stroke(width = strokeWidth, pathEffect = pathEffect),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.AttachFile, null, tint = FormulooOnSurfaceVariant, modifier = Modifier.size(28.dp))
                Text("Joindre un justificatif", fontWeight = FontWeight.Medium, color = FormulooTextPrimary, fontSize = 14.sp)
                Text("(PDF, image)", fontSize = 12.sp, color = FormulooOnSurfaceVariant)
            }
        }

        submitError?.let { Text(it, color = FormulooError, fontSize = 12.sp) }
    }
}

// ── Confirmation ────────────────────────────────────────────────────────────

@Composable
private fun ConfirmationContent(
    formState: LeaveFormState,
    days: Int,
    employeeName: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(FormulooMint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = FormulooPrimary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Demande envoyée ✓", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = FormulooTextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Votre demande a été transmise et sera traitée dans les meilleurs délais.",
            fontSize = 14.sp,
            color = FormulooOnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FormulooMint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.DateRange, null, tint = FormulooPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Récapitulatif", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FormulooTextPrimary)
                }
                Spacer(Modifier.height(12.dp))
                if (employeeName.isNotBlank()) ConfirmRow("Demandeur", employeeName)
                ConfirmRow("Type", formState.type.toLibelleFr())
                val period = if (formState.startDate == formState.endDate) formState.startDate
                             else "${formState.startDate} → ${formState.endDate}"
                ConfirmRow("Période", period)
                ConfirmRow("Durée", "$days jour${if (days > 1) "s" else ""}")
                ConfirmRow("Statut", "En attente", isLast = true)
            }
        }
    }
}

@Composable
private fun ConfirmRow(label: String, value: String, isLast: Boolean = false) {
    HorizontalDivider(thickness = 0.5.dp, color = FormulooOutline.copy(alpha = 0.6f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = FormulooLabelGray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FormulooTextPrimary)
    }
    if (isLast) HorizontalDivider(thickness = 0.5.dp, color = FormulooOutline.copy(alpha = 0.6f))
}

// ── Footer ──────────────────────────────────────────────────────────────────

@Composable
private fun LeaveFooter(
    primaryText: String,
    onPrimary: () -> Unit,
    onBack: (() -> Unit)? = null,
    primaryLoading: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp),
                border = BorderStroke(1.dp, FormulooOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FormulooTextPrimary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", modifier = Modifier.size(20.dp))
            }
        }
        Button(
            onClick = onPrimary,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary, contentColor = Color.White),
            enabled = !primaryLoading,
        ) {
            if (primaryLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(primaryText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
