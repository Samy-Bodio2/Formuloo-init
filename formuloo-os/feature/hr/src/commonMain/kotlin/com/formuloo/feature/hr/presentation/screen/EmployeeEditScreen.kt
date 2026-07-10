package com.formuloo.feature.hr.presentation.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooDropdownField
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeEditUiState
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeEditViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeEditScreen(
    employeeId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EmployeeEditViewModel = koinViewModel(parameters = { parametersOf(employeeId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSaved()
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Modifier l'employé",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FormulooTextPrimary,
                        )
                        if (state.employeeNumber.isNotBlank()) {
                            Text(
                                state.employeeNumber,
                                fontSize = 12.sp,
                                color = FormulooLabelGray,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FormulooBackground),
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FormulooBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = viewModel::submitForm,
                    enabled = !state.isSubmitting && !state.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FormulooPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = FormulooPrimary.copy(alpha = 0.5f),
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Enregistrer les modifications", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FormulooPrimary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 16.dp, vertical = 20.dp)),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Bannière erreur globale
            state.errorMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FormulooError.copy(alpha = 0.08f))
                        .padding(12.dp),
                ) {
                    Text(msg, color = FormulooError, fontSize = 13.sp)
                }
            }

            // Champs en lecture seule
            ReadOnlyChipsRow(state)

            EditSection1_Identite(state, viewModel)
            EditSection2_Coordonnees(state, viewModel)
            EditSection3_PosteRemuneration(state, viewModel)

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chips lecture seule
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReadOnlyChipsRow(state: EmployeeEditUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = FormulooLabelGray,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "Champs non modifiables",
                fontSize = 11.sp,
                color = FormulooLabelGray,
                fontStyle = FontStyle.Italic,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadOnlyChip(label = "Matricule", value = state.employeeNumber)
            ReadOnlyChip(label = "Genre", value = if (state.gender == "M") "Masculin" else if (state.gender == "F") "Féminin" else state.gender)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadOnlyChip(label = "Embauche", value = state.hireDate)
            ReadOnlyChip(label = "Contrat", value = contractTypeLabel(state.typeEmploye))
        }
        Row {
            ReadOnlyChip(label = "Email pro", value = state.emailPro)
        }
    }
}

@Composable
private fun ReadOnlyChip(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(FormulooMint)
            .border(1.dp, FormulooOutline, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label : ", fontSize = 11.sp, color = FormulooLabelGray, fontWeight = FontWeight.Medium)
        Text(value.ifBlank { "—" }, fontSize = 11.sp, color = FormulooPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 1 — Identité
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditSection1_Identite(state: EmployeeEditUiState, viewModel: EmployeeEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EditSectionDivider(number = 1, title = "Identité")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditLabeledField(
                label = "Prénom",
                required = true,
                errorMessage = state.errors["firstName"],
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = viewModel::onFirstNameChange,
                    singleLine = true,
                    isError = state.errors.containsKey("firstName"),
                    shape = RoundedCornerShape(8.dp),
                    colors = editFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            EditLabeledField(
                label = "Nom",
                required = true,
                errorMessage = state.errors["lastName"],
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = viewModel::onLastNameChange,
                    singleLine = true,
                    isError = state.errors.containsKey("lastName"),
                    shape = RoundedCornerShape(8.dp),
                    colors = editFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditLabeledField(label = "Situation familiale", modifier = Modifier.weight(1.4f)) {
                FormulooDropdownField(
                    value = state.situationFamiliale,
                    onValueChange = viewModel::onSituationFamilialeChange,
                    label = "",
                    options = listOf("célibataire", "marié(e)", "divorcé(e)", "veuf/veuve"),
                )
            }
            EditLabeledField(label = "Nb. enfants", modifier = Modifier.weight(0.6f)) {
                OutlinedTextField(
                    value = state.nombreEnfants,
                    onValueChange = viewModel::onNombreEnfantsChange,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = editFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 2 — Coordonnées
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditSection2_Coordonnees(state: EmployeeEditUiState, viewModel: EmployeeEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EditSectionDivider(number = 2, title = "Coordonnées")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditLabeledField(
                label = "Téléphone pro",
                required = true,
                errorMessage = state.errors["phone"],
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    singleLine = true,
                    isError = state.errors.containsKey("phone"),
                    shape = RoundedCornerShape(8.dp),
                    colors = editFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            EditLabeledField(label = "Téléphone perso", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.phonePerso,
                    onValueChange = viewModel::onPhonePersoChange,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = editFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        EditLabeledField(label = "Email personnel") {
            OutlinedTextField(
                value = state.emailPerso,
                onValueChange = viewModel::onEmailPersoChange,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = editFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditLabeledField(label = "Ville", modifier = Modifier.weight(1f)) {
                FormulooDropdownField(
                    value = state.ville,
                    onValueChange = viewModel::onVilleChange,
                    label = "",
                    options = listOf("Douala", "Yaoundé", "Bafoussam", "Garoua", "Maroua", "Bamenda", "Buea", "Edéa"),
                )
            }
            EditLabeledField(label = "Adresse", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::onAddressChange,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = editFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 3 — Poste & rémunération
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditSection3_PosteRemuneration(state: EmployeeEditUiState, viewModel: EmployeeEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EditSectionDivider(number = 3, title = "Poste & rémunération")

        EditLabeledField(label = "Département") {
            FormulooDropdownField(
                value = state.selectedDepartmentName,
                onValueChange = { name ->
                    val dept = state.departments.firstOrNull { it.nom == name }
                    viewModel.onDepartmentSelect(dept?.id ?: "", name)
                },
                label = "",
                options = state.departments.map { it.nom },
            )
        }

        EditLabeledField(label = "Poste") {
            FormulooDropdownField(
                value = state.selectedPosteName,
                onValueChange = { name ->
                    val poste = state.postes.firstOrNull { it.titre == name }
                    viewModel.onPosteSelect(poste?.id ?: "", name)
                },
                label = "",
                options = state.postes.map { it.titre },
            )
        }

        EditLabeledField(label = "Responsable hiérarchique") {
            val options = listOf("— Aucun —") + state.employees
                .filter { it.id != state.employeeNumber }
                .map { it.fullName }
            FormulooDropdownField(
                value = state.selectedManagerName.ifBlank { "— Aucun —" },
                onValueChange = { name ->
                    if (name == "— Aucun —") {
                        viewModel.onManagerSelect("", "")
                    } else {
                        val emp = state.employees.firstOrNull { it.fullName == name }
                        viewModel.onManagerSelect(emp?.id ?: "", name)
                    }
                },
                label = "",
                options = options,
            )
        }

        EditLabeledField(label = "Salaire brut (FCFA)") {
            OutlinedTextField(
                value = state.salaireBase,
                onValueChange = viewModel::onSalaireBaseChange,
                placeholder = { Text("ex : 1 280 000", color = FormulooOnSurfaceVariant, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = editFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants locaux
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditSectionDivider(number: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FormulooPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FormulooTextPrimary)
    }
}

@Composable
private fun EditLabeledField(
    label: String,
    required: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, fontSize = 13.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium)
            if (required) Text(" *", fontSize = 13.sp, color = FormulooError)
        }
        content()
        if (errorMessage != null) {
            Text(errorMessage, color = FormulooError, fontSize = 12.sp, modifier = Modifier.padding(start = 2.dp))
        }
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FormulooPrimary,
    unfocusedBorderColor = FormulooOutline,
    errorBorderColor = FormulooError,
    focusedContainerColor = FormulooSurface,
    unfocusedContainerColor = FormulooSurface,
    errorContainerColor = FormulooSurface,
)

private fun contractTypeLabel(type: String) = when (type.lowercase()) {
    "permanent" -> "CDI"
    "contractuel" -> "CDD"
    "stagiaire" -> "Stage"
    "consultant" -> "Freelance"
    else -> type
}
