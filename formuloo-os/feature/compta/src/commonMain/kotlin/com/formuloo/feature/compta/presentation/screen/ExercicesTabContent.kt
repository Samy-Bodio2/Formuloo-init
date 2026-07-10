package com.formuloo.feature.compta.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.formuloo.core.network.dto.compta.ExerciceCloturerResponseDto
import com.formuloo.core.network.dto.compta.ExerciceDto
import com.formuloo.feature.compta.presentation.viewmodel.ExercicesUiState

@Composable
internal fun ExercicesTab(
    state: ExercicesUiState,
    onRetry: () -> Unit,
    onRequestCloturer: (ExerciceDto) -> Unit,
    onDismissCloturerDialog: () -> Unit,
    onConfirmCloturer: () -> Unit,
    onDismissCloturerResult: () -> Unit,
    onShowCreateDialog: () -> Unit,
    onDismissCreateDialog: () -> Unit,
    onSetCreateAnnee: (String) -> Unit,
    onSetCreateDateDebut: (String) -> Unit,
    onSetCreateDateFin: (String) -> Unit,
    onCreateExercice: () -> Unit,
) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FormulooPrimary)
        }

        state.loadError != null -> Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.loadError, color = FormulooError, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onRetry) { Text("Réessayer", color = FormulooPrimary) }
            }
        }

        state.cloturerResult != null -> CloturerResultScreen(
            result = state.cloturerResult,
            onDismiss = onDismissCloturerResult,
        )

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(FormulooBackground),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // ── Exercice courant ───────────────────────────────────────
                item {
                    ExerciceCourantSection(
                        exercice = state.exerciceOuvert,
                        isCloturing = state.isCloturing,
                        cloturerError = state.cloturerError,
                        peutCreer = state.peutCreerNouveau,
                        onCloturer = { state.exerciceOuvert?.let(onRequestCloturer) },
                        onShowCreate = onShowCreateDialog,
                    )
                }

                // ── Historique ─────────────────────────────────────────────
                if (state.exercicesClotures.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Historique",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = FormulooTextPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${state.exercicesClotures.size} exercice${if (state.exercicesClotures.size > 1) "s" else ""} clôturé${if (state.exercicesClotures.size > 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = FormulooOnSurfaceVariant,
                            )
                        }
                    }

                    items(state.exercicesClotures, key = { it.id }) { exercice ->
                        ExerciceHistoriqueRow(exercice = exercice)
                        HorizontalDivider(
                            color = FormulooOutline.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            // ── Dialogue de confirmation de clôture ────────────────────────
            if (state.showCloturerDialog && state.exerciceACloturer != null) {
                CloturerConfirmDialog(
                    exercice = state.exerciceACloturer,
                    error = state.cloturerError,
                    onConfirm = onConfirmCloturer,
                    onDismiss = onDismissCloturerDialog,
                )
            }

            // ── Dialogue de création ───────────────────────────────────────
            if (state.showCreateDialog) {
                CreateExerciceDialog(
                    annee = state.createAnnee,
                    dateDebut = state.createDateDebut,
                    dateFin = state.createDateFin,
                    isCreating = state.isCreating,
                    error = state.createError,
                    onSetAnnee = onSetCreateAnnee,
                    onSetDateDebut = onSetCreateDateDebut,
                    onSetDateFin = onSetCreateDateFin,
                    onCreate = onCreateExercice,
                    onDismiss = onDismissCreateDialog,
                )
            }
        }
    }
}

// ── Section exercice courant ──────────────────────────────────────────────────

@Composable
private fun ExerciceCourantSection(
    exercice: ExerciceDto?,
    isCloturing: Boolean,
    cloturerError: String?,
    peutCreer: Boolean,
    onCloturer: () -> Unit,
    onShowCreate: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (exercice != null) FormulooMint else FormulooSecondaryBg,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (exercice != null) FormulooPrimary.copy(alpha = 0.3f) else FormulooSecondary.copy(alpha = 0.3f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (exercice != null) FormulooPrimary.copy(alpha = 0.15f) else FormulooSecondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (exercice != null) Icons.Filled.DateRange else Icons.Filled.Warning,
                        null,
                        tint = if (exercice != null) FormulooPrimary else FormulooSecondary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (exercice != null) "Exercice ${exercice.annee}" else "Aucun exercice ouvert",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = FormulooTextPrimary,
                    )
                    if (exercice != null) {
                        Text(
                            "${exercice.dateDebut} → ${exercice.dateFin}",
                            fontSize = 12.sp,
                            color = FormulooPrimary.copy(alpha = 0.7f),
                        )
                    }
                }
                if (exercice != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(FormulooPrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("OUVERT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FormulooPrimary)
                    }
                }
            }

            if (exercice != null) {
                Spacer(Modifier.height(16.dp))
                // Stats rapides
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatPill(
                        label = "Écritures",
                        value = exercice.nbEcritures.toString(),
                        color = FormulooPrimary,
                    )
                    StatPill(
                        label = "Durée",
                        value = "${exercice.dateFin.take(4)}-${exercice.dateDebut.take(4)}",
                        color = FormulooPrimary,
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = FormulooPrimary.copy(alpha = 0.15f), thickness = 0.5.dp)
                Spacer(Modifier.height(14.dp))

                if (cloturerError != null) {
                    Text(
                        cloturerError,
                        color = FormulooError,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FormulooError.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Button(
                    onClick = onCloturer,
                    enabled = !isCloturing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FormulooError),
                ) {
                    if (isCloturing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Clôture en cours…", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Filled.Lock, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clôturer l'exercice", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Créez un nouvel exercice pour commencer la saisie.",
                    fontSize = 13.sp,
                    color = FormulooOnSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onShowCreate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Créer un exercice", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.7f))
    }
}

// ── Ligne historique ──────────────────────────────────────────────────────────

@Composable
private fun ExerciceHistoriqueRow(exercice: ExerciceDto) {
    val resultat = exercice.resultatNet?.toDoubleOrNull()
    val isBenefice = (resultat ?: 0.0) >= 0
    val resultatColor = if (isBenefice) FormulooPrimary else FormulooError
    val resultatLabel = if (isBenefice) "Bénéfice" else "Perte"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FormulooSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FormulooOutline.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, null, tint = FormulooOnSurfaceVariant, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Exercice ${exercice.annee}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = FormulooTextPrimary,
            )
            Text(
                "${exercice.dateDebut} → ${exercice.dateFin}",
                fontSize = 11.sp,
                color = FormulooOnSurfaceVariant,
            )
            if (exercice.dateCloture != null) {
                Text("Clôturé le ${exercice.dateCloture}", fontSize = 11.sp, color = FormulooOnSurfaceVariant)
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            if (resultat != null) {
                Text(
                    formatAmount(kotlin.math.abs(resultat)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = resultatColor,
                )
                Text(resultatLabel, fontSize = 10.sp, color = resultatColor.copy(alpha = 0.7f))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(FormulooOutline.copy(alpha = 0.3f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("CLÔTURÉ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FormulooOnSurfaceVariant)
            }
        }
    }
}

// ── Dialogue de confirmation de clôture ──────────────────────────────────────

@Composable
private fun CloturerConfirmDialog(
    exercice: ExerciceDto,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Warning, null, tint = FormulooError, modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Clôturer l'exercice ${exercice.annee}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cette action est irréversible. La clôture va :",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FormulooTextPrimary,
                )
                listOf(
                    "Vérifier qu'il n'y a aucune écriture en brouillon",
                    "Calculer le résultat net OHADA (Produits − Charges)",
                    "Générer l'écriture de clôture vers le compte 130 ou 139",
                    "Marquer l'exercice ${exercice.annee} comme définitivement clôturé",
                    "Créer automatiquement l'exercice ${exercice.annee + 1}",
                ).forEach { item ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", fontSize = 13.sp, color = FormulooError)
                        Text(item, fontSize = 13.sp, color = FormulooOnSurfaceVariant)
                    }
                }
                if (error != null) {
                    Text(
                        error,
                        color = FormulooError,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FormulooError.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = FormulooError),
            ) {
                Text("Clôturer définitivement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = FormulooOnSurfaceVariant)
            }
        },
    )
}

// ── Écran de résultat de clôture ──────────────────────────────────────────────

@Composable
private fun CloturerResultScreen(result: ExerciceCloturerResponseDto, onDismiss: () -> Unit) {
    val isBenefice = result.typeResultat == "BENEFICE"
    val resultatDouble = result.resultatNet.toDoubleOrNull() ?: 0.0

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isBenefice) FormulooMint else FormulooSecondaryBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    null,
                    tint = if (isBenefice) FormulooPrimary else FormulooSecondary,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Exercice ${result.exercice.annee} clôturé",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = FormulooTextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            // Résultat net
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBenefice) FormulooMint else FormulooSecondaryBg,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBenefice) FormulooPrimary.copy(alpha = 0.3f) else FormulooSecondary.copy(alpha = 0.3f),
                ),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (isBenefice) "Résultat net — BÉNÉFICE" else "Résultat net — PERTE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isBenefice) FormulooPrimary else FormulooSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${formatAmount(kotlin.math.abs(resultatDouble))} XAF",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBenefice) FormulooPrimary else FormulooSecondary,
                    )
                    Text(
                        "Comptabilisé au compte ${if (isBenefice) "130" else "139"}",
                        fontSize = 11.sp,
                        color = FormulooOnSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Exercice suivant
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = FormulooSurface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, null, tint = FormulooPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Exercice ${result.exerciceSuivant.annee} " +
                                if (result.exerciceSuivant.cree) "créé automatiquement" else "déjà existant",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FormulooTextPrimary,
                        )
                        Text("Statut : OUVERT", fontSize = 11.sp, color = FormulooPrimary)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Retour aux exercices", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Dialogue de création ──────────────────────────────────────────────────────

@Composable
private fun CreateExerciceDialog(
    annee: String,
    dateDebut: String,
    dateFin: String,
    isCreating: Boolean,
    error: String?,
    onSetAnnee: (String) -> Unit,
    onSetDateDebut: (String) -> Unit,
    onSetDateFin: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        icon = {
            Icon(Icons.Filled.DateRange, null, tint = FormulooPrimary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("Nouvel exercice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExoDialogField(
                    label = "Année *",
                    value = annee,
                    hint = "Ex : 2025",
                    enabled = !isCreating,
                    onValueChange = onSetAnnee,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
                ExoDialogField(
                    label = "Date de début *",
                    value = dateDebut,
                    hint = "AAAA-MM-JJ",
                    enabled = !isCreating,
                    onValueChange = onSetDateDebut,
                )
                ExoDialogField(
                    label = "Date de fin *",
                    value = dateFin,
                    hint = "AAAA-MM-JJ",
                    enabled = !isCreating,
                    onValueChange = onSetDateFin,
                )

                if (error != null) {
                    Text(
                        error,
                        color = FormulooError,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FormulooError.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(10.dp),
                    )
                }

                if (isCreating) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FormulooPrimary)
                        Text("Création en cours…", fontSize = 13.sp, color = FormulooPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Annuler", color = FormulooOnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun ExoDialogField(
    label: String,
    value: String,
    hint: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
) {
    Column {
        Text(label, fontSize = 11.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = if (enabled) onValueChange else { _ -> },
            readOnly = !enabled,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = FormulooTextPrimary),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(FormulooBackground, RoundedCornerShape(8.dp))
                .border(1.dp, FormulooOutline, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, fontSize = 14.sp, color = FormulooOnSurfaceVariant.copy(alpha = 0.6f))
                }
                inner()
            },
        )
    }
}
