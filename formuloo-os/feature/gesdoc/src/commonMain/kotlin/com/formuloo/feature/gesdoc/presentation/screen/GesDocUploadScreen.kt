package com.formuloo.feature.gesdoc.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.formuloo.core.designsystem.FormulooTeal
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.StepProgressBar
import com.formuloo.feature.gesdoc.domain.model.DocumentType
import com.formuloo.feature.gesdoc.presentation.components.GesDocTabBar
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocUploadState
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocUploadViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesDocUploadScreen(
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onNavigateToAudit: () -> Unit = {},
    onSubmitSuccess: (String) -> Unit,
    viewModel: GesDocUploadViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.uploadedDocumentId) {
        val id = state.uploadedDocumentId ?: return@LaunchedEffect
        viewModel.clearUploadedId()
        onSubmitSuccess(id)
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
            SubmitFooter(state = state, onSubmit = viewModel::submit)
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Barre de progression 4 étapes
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        StepProgressBar(totalSteps = state.totalSteps, currentStep = state.currentStep)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            buildAnnotatedString {
                                append("Étape ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(state.currentStep.toString())
                                }
                                append(" sur ${state.totalSteps} · Upload")
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
                            "Soumettre une pièce",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = FormulooTextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Déposez la pièce comptable — elle sera lue automatiquement par OCR.",
                            fontSize = 13.sp,
                            color = FormulooOnSurfaceVariant,
                        )
                    }
                }

                // Zone de dépôt de fichier
                item {
                    Spacer(Modifier.height(12.dp))
                    FileDropZone(
                        state = state,
                        onTap = onPickFile,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                // Section "TYPE DE PIÈCE"
                item {
                    Spacer(Modifier.height(20.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "TYPE DE PIÈCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FormulooOnSurfaceVariant,
                            letterSpacing = 0.8.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DocumentTypeCard(
                                type = DocumentType.INVOICE,
                                selected = state.selectedType == DocumentType.INVOICE,
                                onClick = { viewModel.selectDocumentType(DocumentType.INVOICE) },
                                modifier = Modifier.weight(1f),
                            )
                            DocumentTypeCard(
                                type = DocumentType.PURCHASE_ORDER,
                                selected = state.selectedType == DocumentType.PURCHASE_ORDER,
                                onClick = { viewModel.selectDocumentType(DocumentType.PURCHASE_ORDER) },
                                modifier = Modifier.weight(1f),
                            )
                            DocumentTypeCard(
                                type = DocumentType.RECEIPT,
                                selected = state.selectedType == DocumentType.RECEIPT,
                                onClick = { viewModel.selectDocumentType(DocumentType.RECEIPT) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Ligne récapitulative du fichier sélectionné
                if (state.fileName != null) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        FileSummaryRow(
                            state = state,
                            onClear = viewModel::clearFile,
                            modifier = Modifier.padding(horizontal = 16.dp),
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

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ── Zone de dépôt ─────────────────────────────────────────────────────────────

@Composable
private fun FileDropZone(
    state: GesDocUploadState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = FormulooPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FormulooMint)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(14f, 7f),
                            phase = 0f,
                        ),
                    ),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                )
            }
            .clickable { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        if (state.fileName != null) {
            // État : fichier sélectionné
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FormulooTeal),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text(
                    "Fichier sélectionné",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FormulooTextPrimary,
                )
                Text(
                    state.fileName,
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }
        } else {
            // État vide : aucun fichier choisi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.CloudUpload,
                    contentDescription = null,
                    tint = FormulooTeal,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    "Appuyez pour sélectionner un fichier",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = FormulooTextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "PDF, image · max 10 Mo",
                    fontSize = 12.sp,
                    color = FormulooOnSurfaceVariant,
                )
            }
        }
    }
}

// ── Cartes de sélection du type de pièce ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentTypeCard(
    type: DocumentType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) FormulooPrimary else FormulooOutline
    val bgColor = if (selected) FormulooMint else FormulooSurface
    val contentColor = if (selected) FormulooPrimary else FormulooOnSurfaceVariant
    val icon = when (type) {
        DocumentType.INVOICE -> Icons.Filled.Description
        DocumentType.PURCHASE_ORDER -> Icons.Filled.ShoppingCart
        DocumentType.RECEIPT -> Icons.Filled.CreditCard
        DocumentType.UNKNOWN -> Icons.Filled.Description
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = type.label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Ligne récapitulative du fichier ───────────────────────────────────────────

@Composable
private fun FileSummaryRow(
    state: GesDocUploadState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FormulooSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Badge icône orange (style PDF)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FormulooSecondaryBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                tint = FormulooSecondary,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                state.fileName ?: "",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = FormulooTextPrimary,
                maxLines = 1,
            )
            Text(
                "${state.fileSizeLabel} · aperçu prêt · ${state.selectedType.label}",
                fontSize = 11.sp,
                color = FormulooOnSurfaceVariant,
            )
        }

        IconButton(onClick = onClear) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Supprimer",
                tint = FormulooError,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Bouton footer fixe ────────────────────────────────────────────────────────

@Composable
private fun SubmitFooter(state: GesDocUploadState, onSubmit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FormulooPrimary,
                disabledContainerColor = FormulooOutline,
            ),
        ) {
            if (state.isUploading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    "Soumettre",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (state.canSubmit) Color.White else FormulooOnSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (state.canSubmit) Color.White else FormulooOnSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
