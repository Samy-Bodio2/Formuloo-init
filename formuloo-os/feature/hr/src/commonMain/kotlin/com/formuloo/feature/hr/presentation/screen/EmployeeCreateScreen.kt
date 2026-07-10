package com.formuloo.feature.hr.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.formuloo.core.designsystem.FormulooError
import com.formuloo.core.designsystem.FormulooLabelGray
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutline
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooSurface
import com.formuloo.core.designsystem.FormulooTextPrimary
import com.formuloo.core.designsystem.FormulooDropdownField
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeCreateUiState
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeCreateViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeCreateScreen(
    onBack: () -> Unit,
    onCreated: (employeeId: String) -> Unit,
    viewModel: EmployeeCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onCreated(state.createdEmployeeId ?: "")
    }

    Scaffold(
        containerColor = FormulooBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nouvel employé",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FormulooTextPrimary,
                        )
                        Text(
                            text = "Ressources Humaines",
                            fontSize = 12.sp,
                            color = FormulooLabelGray,
                        )
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
                    enabled = !state.isSubmitting,
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
                            Text("Créer l'employé", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 16.dp, vertical = 20.dp)),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Bannière d'erreur globale
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

            Section1_CompteUtilisateur(state, viewModel)
            Section2_Identite(state, viewModel)
            Section3_Coordonnees(state, viewModel)
            Section4_PosteContrat(state, viewModel)

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 1 — Compte utilisateur
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Section1_CompteUtilisateur(
    state: EmployeeCreateUiState,
    viewModel: EmployeeCreateViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionDivider(number = 1, title = "Compte utilisateur", subtitle = "Étape recommandée")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FormulooSurface),
            elevation = CardDefaults.cardElevation(1.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // En-tête de la carte
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FormulooMint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = FormulooPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Lier à un compte existant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = FormulooTextPrimary,
                    )
                }

                // Description avec badge code inline
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Via ", fontSize = 13.sp, color = FormulooLabelGray)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FormulooBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "GET /utilisateurs/",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = FormulooPrimary,
                        )
                    }
                    Text(
                        ". Si le compte existe, il est rattaché à l'employé.",
                        fontSize = 13.sp,
                        color = FormulooLabelGray,
                    )
                }

                // Champ email + bouton Vérifier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.linkedUserEmail,
                        onValueChange = viewModel::onLinkedUserEmailChange,
                        placeholder = {
                            Text(
                                "prenom.nom@entreprise",
                                color = FormulooOnSurfaceVariant,
                                fontSize = 13.sp,
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = viewModel::verifyUserAccount,
                        enabled = !state.isVerifyingUser,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FormulooPrimary),
                    ) {
                        if (state.isVerifyingUser) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Vérifier", fontSize = 13.sp)
                        }
                    }
                }

                // Compte trouvé
                if (state.linkedUserName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = FormulooPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Compte trouvé : ${state.linkedUserName}",
                            fontSize = 13.sp,
                            color = FormulooPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Erreur de vérification
                if (state.userVerifyError != null) {
                    Text(state.userVerifyError, fontSize = 12.sp, color = FormulooError)
                }
            }
        }

        // Texte d'aide
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = FormulooLabelGray,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Facultatif — laissez vide pour créer sans compte lié.",
                fontSize = 12.sp,
                color = FormulooLabelGray,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 2 — Identité
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Section2_Identite(
    state: EmployeeCreateUiState,
    viewModel: EmployeeCreateViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionDivider(number = 2, title = "Identité")

        // Prénom + Nom côte à côte
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LabeledField(
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
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            LabeledField(
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
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Genre (boutons segmentés pleine largeur)
        LabeledField(label = "Genre") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("M" to "Masculin", "F" to "Féminin").forEach { (value, label) ->
                    GenderButton(
                        label = label,
                        selected = state.gender == value,
                        onClick = { viewModel.onGenderChange(value) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 3 — Coordonnées
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Section3_Coordonnees(
    state: EmployeeCreateUiState,
    viewModel: EmployeeCreateViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionDivider(number = 3, title = "Coordonnées")

        LabeledField(
            label = "E-mail professionnel",
            required = true,
            errorMessage = state.errors["emailPro"],
        ) {
            OutlinedTextField(
                value = state.emailPro,
                onValueChange = viewModel::onEmailProChange,
                singleLine = true,
                isError = state.errors.containsKey("emailPro"),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        LabeledField(
            label = "Téléphone",
            required = true,
            errorMessage = state.errors["phone"],
        ) {
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                singleLine = true,
                isError = state.errors.containsKey("phone"),
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section 4 — Poste & contrat
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Section4_PosteContrat(
    state: EmployeeCreateUiState,
    viewModel: EmployeeCreateViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionDivider(number = 4, title = "Poste & contrat")

        // Localisation
        LabeledField(label = "Localisation") {
            FormulooDropdownField(
                value = state.ville,
                onValueChange = viewModel::onVilleChange,
                label = "",
                options = listOf("Douala", "Yaoundé", "Bafoussam", "Garoua", "Maroua", "Bamenda", "Buea", "Edéa"),
            )
        }

        // Département
        LabeledField(label = "Département") {
            val deptOptions = state.departments.map { it.nom }
            FormulooDropdownField(
                value = state.selectedDepartmentName,
                onValueChange = { name ->
                    val dept = state.departments.firstOrNull { it.nom == name }
                    viewModel.onDepartmentSelect(dept?.id ?: "", name)
                },
                label = "",
                options = deptOptions,
            )
        }

        // Poste
        LabeledField(label = "Poste") {
            val posteOptions = state.postes.map { it.titre }
            FormulooDropdownField(
                value = state.selectedPosteName,
                onValueChange = { name ->
                    val poste = state.postes.firstOrNull { it.titre == name }
                    viewModel.onPosteSelect(poste?.id ?: "", name)
                },
                label = "",
                options = posteOptions,
            )
        }

        // Responsable hiérarchique
        LabeledField(label = "Responsable hiérarchique") {
            val managerOptions = listOf("— Aucun —") + state.employees.map { it.fullName }
            FormulooDropdownField(
                value = state.selectedManagerName,
                onValueChange = { name ->
                    if (name == "— Aucun —") {
                        viewModel.onManagerSelect("", "")
                    } else {
                        val emp = state.employees.firstOrNull { it.fullName == name }
                        viewModel.onManagerSelect(emp?.id ?: "", name)
                    }
                },
                label = "",
                options = managerOptions,
            )
        }

        // Type de contrat
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                Text("Type de contrat", fontSize = 13.sp, color = FormulooOnSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "permanent" to "CDI",
                    "contractuel" to "CDD",
                    "stagiaire" to "Stage",
                    "consultant" to "Freelance",
                ).forEach { (value, label) ->
                    ContractTypeChip(
                        label = label,
                        selected = state.contractType == value,
                        onClick = { viewModel.onContractTypeChange(value) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Date d'embauche + Salaire brut (côte à côte)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LabeledField(
                label = "Date d'embauche",
                required = true,
                errorMessage = state.errors["hireDate"],
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = state.hireDate,
                    onValueChange = viewModel::onHireDateChange,
                    placeholder = { Text("YYYY-MM-DD", color = FormulooOnSurfaceVariant, fontSize = 13.sp) },
                    singleLine = true,
                    isError = state.errors.containsKey("hireDate"),
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            LabeledField(
                label = "Salaire brut (FCFA)",
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = state.salaireBase,
                    onValueChange = viewModel::onSalaireBaseChange,
                    placeholder = { Text("1 280 000", color = FormulooOnSurfaceVariant, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants réutilisables locaux
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionDivider(number: Int, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FormulooPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$number",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = FormulooTextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = FormulooLabelGray,
                )
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    required: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(
                text = label,
                fontSize = 13.sp,
                color = FormulooOnSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            if (required) {
                Text(" *", fontSize = 13.sp, color = FormulooError)
            }
        }
        content()
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = FormulooError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

@Composable
private fun GenderButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) FormulooSurface else Color(0xFFF0F0F0))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) FormulooPrimary else FormulooOutline,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (selected) FormulooPrimary else FormulooOnSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ContractTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FormulooSurface)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) FormulooPrimary else FormulooOutline,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) FormulooPrimary else FormulooOnSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FormulooPrimary,
    unfocusedBorderColor = FormulooOutline,
    errorBorderColor = FormulooError,
    focusedContainerColor = FormulooSurface,
    unfocusedContainerColor = FormulooSurface,
    errorContainerColor = FormulooSurface,
)
