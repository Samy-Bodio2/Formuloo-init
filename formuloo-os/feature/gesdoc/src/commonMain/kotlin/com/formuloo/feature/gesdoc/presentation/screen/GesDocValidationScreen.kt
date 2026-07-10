package com.formuloo.feature.gesdoc.presentation.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.BadgeTone
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooErrorBg
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTeal
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.StatusBadge
import com.formuloo.core.designsystem.StepProgressBar
import com.formuloo.feature.gesdoc.presentation.components.GesDocTabBar
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocValidationState
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocValidationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesDocValidationScreen(
    documentId: String,
    onBack: () -> Unit,
    onNavigateToAudit: () -> Unit = {},
    onValidated: (String) -> Unit,
    viewModel: GesDocValidationViewModel = koinViewModel(parameters = { parametersOf(documentId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var hasAutoAdvanced by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isValidated) {
        if (state.isValidated && !hasAutoAdvanced) {
            hasAutoAdvanced = true
            onValidated(documentId)
        }
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Gestion documentaire",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FormulooTextPrimary,
                        )
                        Text(
                            "OCR · certification Blockchain",
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = FormulooTextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filtrer",
                            tint = FormulooOnSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooSurface),
            )
        },
        bottomBar = {
            ValidationFooter(
                onBack = onBack,
                onValidate = viewModel::validate,
                isSaving = state.isSaving,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            GesDocTabBar(
                activeTabIndex = 1,
                onNavigateToDashboard = onBack,
                onNavigateToUpload = {},
                onNavigateToAudit = onNavigateToAudit,
            )

            if (state.isLoading && state.ocrResult == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FormulooPrimary)
                }
            } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // Barre de progression 4 étapes
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        StepProgressBar(totalSteps = 4, currentStep = 3)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            buildAnnotatedString {
                                append("Étape ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("3") }
                                append(" sur 4 · Validation")
                            },
                            fontSize = 12.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }

                // Titre + sous-titre de l'étape
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            "Vérifiez les données extraites",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = FormulooTextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Corrigez les champs si nécessaire avant certification.",
                            fontSize = 13.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }

                // Carte d'aperçu du document original
                item {
                    Spacer(Modifier.height(12.dp))
                    DocumentPreviewCard(state = state, modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Champs de validation OCR
                item {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ValidationField(
                            label = "NUMÉRO DE DOCUMENT",
                            value = state.documentNumber,
                            confidence = state.documentNumberConfidence,
                            onValueChange = viewModel::onDocumentNumberChange,
                            monospace = true,
                        )
                        ValidationField(
                            label = "DATE",
                            value = state.date,
                            confidence = state.dateConfidence,
                            onValueChange = viewModel::onDateChange,
                            monospace = true,
                        )
                        ValidationField(
                            label = "FOURNISSEUR",
                            value = state.supplier,
                            confidence = state.supplierConfidence,
                            onValueChange = viewModel::onSupplierChange,
                            monospace = false,
                        )
                        ValidationField(
                            label = "MONTANT HT",
                            value = state.amountHt,
                            confidence = state.amountHtConfidence,
                            onValueChange = viewModel::onAmountHtChange,
                            monospace = true,
                        )
                        ValidationField(
                            label = "TAUX TVA (%)",
                            value = state.tvaRate,
                            confidence = state.tvaRateConfidence,
                            onValueChange = viewModel::onTvaRateChange,
                            monospace = true,
                        )
                        ValidationField(
                            label = "MONTANT TTC",
                            value = state.amountTtc,
                            confidence = state.amountTtcConfidence,
                            onValueChange = viewModel::onAmountTtcChange,
                            monospace = true,
                        )
                    }
                }

                // Bannière erreur
                if (state.error != null) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(FormulooErrorBg, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = FormulooError,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(state.error!!, fontSize = 13.sp, color = FormulooError)
                        }
                    }
                }
            }
            }
        }
    }
}

// ── Carte d'aperçu du document original ──────────────────────────────────────

@Composable
private fun DocumentPreviewCard(state: GesDocValidationState, modifier: Modifier = Modifier) {
    val amountHt = state.amountHt.toDoubleOrNull()
    val tvaRate = state.tvaRate.toDoubleOrNull()
    val amountTtc = state.amountTtc.toDoubleOrNull()
    val tvaAmount = if (amountHt != null && tvaRate != null) amountHt * tvaRate / 100 else null

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FormulooSurface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = state.supplier.ifBlank { "FOURNISSEUR" }.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FormulooTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(label = "ORIGINAL", tone = BadgeTone.NEUTRAL)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "Facture N° ${state.documentNumber} · ${state.date}",
                fontSize = 12.sp,
                color = FormulooOnSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (amountHt != null) {
                PreviewRow(label = "Montant HT", value = formatAmount(amountHt))
                HorizontalDivider(color = FormulooOutline.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
            if (tvaAmount != null && tvaRate != null) {
                PreviewRow(
                    label = "TVA ${formatRate(tvaRate)} %",
                    value = formatAmount(tvaAmount),
                )
                HorizontalDivider(color = FormulooOutline.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
            if (amountTtc != null) {
                PreviewRow(
                    label = "Net à payer TTC",
                    value = "${formatAmount(amountTtc)} ${state.currency}",
                    emphasized = true,
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = FormulooTextPrimary,
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = FormulooTextPrimary,
        )
    }
}

// ── Champ de validation OCR (label + badge de confiance + champ éditable) ────

@Composable
private fun ValidationField(
    label: String,
    value: String,
    confidence: Int?,
    onValueChange: (String) -> Unit,
    monospace: Boolean,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = FormulooOnSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f),
            )
            if (confidence != null) {
                StatusBadge(
                    label = "$confidence %",
                    tone = confidenceTone(confidence),
                    dot = true,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                color = FormulooTextPrimary,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FormulooTeal,
                unfocusedBorderColor = FormulooOutline,
                focusedContainerColor = FormulooSurface,
                unfocusedContainerColor = FormulooSurface,
                cursorColor = FormulooTeal,
            ),
        )
    }
}

private fun confidenceTone(confidence: Int): BadgeTone = when {
    confidence >= 90 -> BadgeTone.SUCCESS
    confidence >= 70 -> BadgeTone.WARNING
    else -> BadgeTone.DANGER
}

// ── Footer fixe : retour + validation ─────────────────────────────────────────

@Composable
private fun ValidationFooter(
    onBack: () -> Unit,
    onValidate: () -> Unit,
    isSaving: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.size(52.dp),
            contentPadding = PaddingValues(0.dp),
            border = BorderStroke(1.dp, FormulooOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FormulooTextPrimary),
            shape = RoundedCornerShape(26.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                modifier = Modifier.size(20.dp),
            )
        }
        Button(
            onClick = onValidate,
            enabled = !isSaving,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FormulooPrimary,
                disabledContainerColor = FormulooOutline,
            ),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    "Valider les données",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ── Helpers de formatage ──────────────────────────────────────────────────────

private fun formatAmount(value: Double): String {
    val rounded = value.toLong()
    val sb = StringBuilder()
    rounded.toString().reversed().forEachIndexed { i, c ->
        if (i > 0 && i % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return sb.toString().reversed()
}

private fun formatRate(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
