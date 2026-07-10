package com.formuloo.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formuloo.core.common.UiState
import com.formuloo.core.designsystem.BusinessIcon
import com.formuloo.core.designsystem.FormulooBackground
import com.formuloo.core.designsystem.FormulooButton
import com.formuloo.core.designsystem.FormulooDropdownField
import com.formuloo.core.designsystem.FormulooMint
import com.formuloo.core.designsystem.FormulooOnSurfaceVariant
import com.formuloo.core.designsystem.FormulooOutlinedButton
import com.formuloo.core.designsystem.FormulooPasswordField
import com.formuloo.core.designsystem.FormulooPrimary
import com.formuloo.core.designsystem.FormulooTextField
import com.formuloo.core.designsystem.PasswordStrengthIndicator
import com.formuloo.core.designsystem.StepProgressBar
import com.formuloo.core.designsystem.SuccessCheckmarkIllustration
import org.koin.compose.viewmodel.koinViewModel

private val sectorOptions = listOf(
    "Technologie",
    "Finance",
    "Santé",
    "Commerce / Distribution",
    "Industrie",
    "Éducation",
    "Services",
    "Immobilier",
    "Autre",
)

private val countryOptions = listOf(
    "France",
    "Côte d'Ivoire",
    "Sénégal",
    "Cameroun",
    "Mali",
    "Burkina Faso",
    "Maroc",
    "Tunisie",
    "Belgique",
    "Canada",
    "Autre",
)

private val secretQuestionOptions = listOf(
    "Quel est le nom de votre premier animal de compagnie ?",
    "Quelle est votre ville de naissance ?",
    "Quel est le nom de jeune fille de votre mère ?",
    "Quel est votre plat préféré ?",
    "Quel était le nom de votre école primaire ?",
)

private val companySizes = listOf("1-10", "11-50", "51-200", "200+")

private fun totalSteps(accountType: AccountType): Int =
    if (accountType == AccountType.PARTICULIER) 4 else 5

private fun otpStep(accountType: AccountType): Int =
    if (accountType == AccountType.PARTICULIER) 3 else 4

private fun stepLabel(accountType: AccountType, step: Int): String = when (accountType) {
    AccountType.PARTICULIER -> when (step) {
        1 -> "Vos informations"
        2 -> "Sécurisez votre compte"
        3 -> "Vérification e-mail"
        4 -> "Votre espace est prêt !"
        else -> ""
    }
    AccountType.ENTREPRISE -> when (step) {
        1 -> "Votre entreprise"
        2 -> "Votre compte administrateur"
        3 -> "Sécurisez votre compte"
        4 -> "Vérification e-mail"
        5 -> "Votre espace est prêt !"
        else -> ""
    }
}

private fun isValidEmail(email: String): Boolean =
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)

private fun validateSecurityStep(form: RegistrationFormData): Map<String, String> = buildMap {
    if (passwordStrength(form.password) != PasswordStrength.FORT) {
        put("password", "Le mot de passe doit être de niveau \"Fort\"")
    }
    if (form.passwordConfirm.isBlank() || form.passwordConfirm != form.password) {
        put("passwordConfirm", "Les mots de passe ne correspondent pas")
    }
    if (form.secretQuestion.isBlank()) put("secretQuestion", "Veuillez sélectionner une question secrète")
    if (form.secretAnswer.isBlank()) put("secretAnswer", "La réponse secrète est requise")
}

private fun validateStep(accountType: AccountType, step: Int, form: RegistrationFormData): Map<String, String> =
    when (accountType) {
        AccountType.PARTICULIER -> when (step) {
            1 -> buildMap {
                if (form.firstName.isBlank()) put("firstName", "Le prénom est requis")
                if (form.lastName.isBlank()) put("lastName", "Le nom est requis")
                if (!isValidEmail(form.email)) put("email", "Adresse e-mail invalide")
            }
            2 -> validateSecurityStep(form)
            else -> emptyMap()
        }
        AccountType.ENTREPRISE -> when (step) {
            1 -> buildMap {
                if (form.companyName.isBlank()) put("companyName", "La raison sociale est requise")
                if (form.sector.isBlank()) put("sector", "Le secteur d'activité est requis")
                if (form.companySize.isBlank()) put("companySize", "Veuillez sélectionner une taille")
                if (form.country.isBlank()) put("country", "Le pays est requis")
            }
            2 -> buildMap {
                if (form.firstName.isBlank()) put("firstName", "Le prénom est requis")
                if (form.lastName.isBlank()) put("lastName", "Le nom est requis")
                if (!isValidEmail(form.email)) put("email", "Adresse e-mail invalide")
            }
            3 -> validateSecurityStep(form)
            else -> emptyMap()
        }
    }

@Composable
fun RegistrationScreen(
    accountType: AccountType,
    onBack: () -> Unit,
    onRegistrationComplete: () -> Unit,
    viewModel: RegistrationViewModel = koinViewModel(),
) {
    val form by viewModel.formData.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val fieldErrors by viewModel.fieldErrors.collectAsStateWithLifecycle()
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()
    val steps = totalSteps(accountType)
    val lastFormStep = otpStep(accountType) - 1

    LaunchedEffect(accountType) {
        viewModel.setAccountType(accountType)
    }

    LaunchedEffect(registerState) {
        if (registerState is UiState.Success) {
            viewModel.consumeRegisterState()
            viewModel.nextStep(steps)
        }
    }

    when (currentStep) {
        otpStep(accountType) -> {
            OtpVerificationScreen(
                email = form.email,
                onVerified = { viewModel.nextStep(steps) },
                onBack = { viewModel.previousStep() },
            )
            return
        }
        steps -> {
            RegistrationSuccessScreen(form = form, onContinue = onRegistrationComplete)
            return
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooBackground),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("< Précédent", color = FormulooPrimary)
                }
                AccountTypeBadge(accountType)
            }
            Spacer(Modifier.height(8.dp))
            StepProgressBar(totalSteps = steps, currentStep = currentStep)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Étape $currentStep sur $steps · ${stepLabel(accountType, currentStep)}",
                style = MaterialTheme.typography.labelSmall,
                color = FormulooOnSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            when (accountType) {
                AccountType.PARTICULIER -> ParticulierStepContent(currentStep, form, fieldErrors, viewModel::updateForm)
                AccountType.ENTREPRISE -> EntrepriseStepContent(currentStep, form, fieldErrors, viewModel::updateForm)
            }
            Spacer(Modifier.height(24.dp))

            if (registerState is UiState.Error) {
                Text(
                    text = (registerState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (currentStep > 1) {
                FormulooOutlinedButton(
                    text = "< Retour",
                    onClick = { viewModel.previousStep() },
                    modifier = Modifier.weight(1f),
                )
            }
            FormulooButton(
                text = "Continuer",
                loading = registerState is UiState.Loading,
                modifier = Modifier.weight(1f),
                onClick = {
                    val errors = validateStep(accountType, currentStep, form)
                    viewModel.setFieldErrors(errors)
                    if (errors.isEmpty()) {
                        if (currentStep == lastFormStep) {
                            viewModel.register()
                        } else {
                            viewModel.nextStep(steps)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun AccountTypeBadge(accountType: AccountType) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FormulooMint)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accountType == AccountType.PARTICULIER) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = FormulooPrimary,
                modifier = Modifier.size(16.dp),
            )
        } else {
            BusinessIcon(tint = FormulooPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(accountType.title, style = MaterialTheme.typography.labelSmall, color = FormulooPrimary)
    }
}

@Composable
private fun ParticulierStepContent(
    step: Int,
    form: RegistrationFormData,
    errors: Map<String, String>,
    onUpdate: ((RegistrationFormData) -> RegistrationFormData) -> Unit,
) {
    when (step) {
        1 -> PersonalInfoStepContent(title = "Vos informations", form = form, errors = errors, onUpdate = onUpdate)
        2 -> SecurityStepContent(form, errors, onUpdate)
    }
}

@Composable
private fun EntrepriseStepContent(
    step: Int,
    form: RegistrationFormData,
    errors: Map<String, String>,
    onUpdate: ((RegistrationFormData) -> RegistrationFormData) -> Unit,
) {
    when (step) {
        1 -> CompanyStepContent(form, errors, onUpdate)
        2 -> PersonalInfoStepContent(
            title = "Votre compte administrateur",
            form = form,
            errors = errors,
            onUpdate = onUpdate,
            emailHelperText = "Cette adresse sera votre identifiant de connexion administrateur.",
        )
        3 -> SecurityStepContent(form, errors, onUpdate)
    }
}

@Composable
private fun CompanyStepContent(
    form: RegistrationFormData,
    errors: Map<String, String>,
    onUpdate: ((RegistrationFormData) -> RegistrationFormData) -> Unit,
) {
    Text("Votre entreprise", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    FormulooTextField(
        value = form.companyName,
        onValueChange = { value -> onUpdate { it.copy(companyName = value) } },
        label = "Raison sociale",
        isError = errors.containsKey("companyName"),
        errorMessage = errors["companyName"],
    )
    Spacer(Modifier.height(16.dp))
    FormulooDropdownField(
        value = form.sector,
        onValueChange = { value -> onUpdate { it.copy(sector = value) } },
        label = "Secteur d'activité",
        options = sectorOptions,
        isError = errors.containsKey("sector"),
        errorMessage = errors["sector"],
    )
    Spacer(Modifier.height(16.dp))
    Text("Taille de l'entreprise", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        companySizes.forEach { size ->
            FilterChip(
                selected = form.companySize == size,
                onClick = { onUpdate { it.copy(companySize = size) } },
                label = { Text(size) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FormulooMint,
                    selectedLabelColor = FormulooPrimary,
                ),
            )
        }
    }
    if (errors.containsKey("companySize")) {
        Text(
            text = errors["companySize"].orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    FormulooDropdownField(
        value = form.country,
        onValueChange = { value -> onUpdate { it.copy(country = value) } },
        label = "Pays",
        options = countryOptions,
        isError = errors.containsKey("country"),
        errorMessage = errors["country"],
    )
    Spacer(Modifier.height(16.dp))
    FormulooTextField(
        value = form.city,
        onValueChange = { value -> onUpdate { it.copy(city = value) } },
        label = "Ville",
    )
}

@Composable
private fun PersonalInfoStepContent(
    title: String,
    form: RegistrationFormData,
    errors: Map<String, String>,
    onUpdate: ((RegistrationFormData) -> RegistrationFormData) -> Unit,
    emailHelperText: String? = null,
) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    FormulooTextField(
        value = form.firstName,
        onValueChange = { value -> onUpdate { it.copy(firstName = value) } },
        label = "Prénom",
        isError = errors.containsKey("firstName"),
        errorMessage = errors["firstName"],
    )
    Spacer(Modifier.height(16.dp))
    FormulooTextField(
        value = form.lastName,
        onValueChange = { value -> onUpdate { it.copy(lastName = value) } },
        label = "Nom",
        isError = errors.containsKey("lastName"),
        errorMessage = errors["lastName"],
    )
    Spacer(Modifier.height(16.dp))
    FormulooTextField(
        value = form.email,
        onValueChange = { value -> onUpdate { it.copy(email = value) } },
        label = "Adresse e-mail",
        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
        isError = errors.containsKey("email"),
        errorMessage = errors["email"],
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    )
    if (emailHelperText != null && !errors.containsKey("email")) {
        Text(
            text = emailHelperText,
            style = MaterialTheme.typography.labelSmall,
            color = FormulooOnSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    FormulooTextField(
        value = form.phone,
        onValueChange = { value -> onUpdate { it.copy(phone = value) } },
        label = "Téléphone",
        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
}

@Composable
private fun SecurityStepContent(
    form: RegistrationFormData,
    errors: Map<String, String>,
    onUpdate: ((RegistrationFormData) -> RegistrationFormData) -> Unit,
) {
    Text("Sécurisez votre compte", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(16.dp))
    FormulooPasswordField(
        value = form.password,
        onValueChange = { value -> onUpdate { it.copy(password = value) } },
        label = "Mot de passe",
        isError = errors.containsKey("password"),
        errorMessage = errors["password"],
    )
    Spacer(Modifier.height(8.dp))
    PasswordStrengthIndicator(password = form.password)
    Spacer(Modifier.height(16.dp))
    FormulooPasswordField(
        value = form.passwordConfirm,
        onValueChange = { value -> onUpdate { it.copy(passwordConfirm = value) } },
        label = "Confirmer le mot de passe",
        isError = errors.containsKey("passwordConfirm"),
        errorMessage = errors["passwordConfirm"],
    )
    Spacer(Modifier.height(16.dp))
    FormulooDropdownField(
        value = form.secretQuestion,
        onValueChange = { value -> onUpdate { it.copy(secretQuestion = value) } },
        label = "Question secrète",
        options = secretQuestionOptions,
        isError = errors.containsKey("secretQuestion"),
        errorMessage = errors["secretQuestion"],
    )
    Spacer(Modifier.height(16.dp))
    FormulooTextField(
        value = form.secretAnswer,
        onValueChange = { value -> onUpdate { it.copy(secretAnswer = value) } },
        label = "Réponse secrète",
        isError = errors.containsKey("secretAnswer"),
        errorMessage = errors["secretAnswer"],
    )
}

@Composable
private fun RegistrationSuccessScreen(
    form: RegistrationFormData,
    onContinue: () -> Unit,
) {
    val subtitle = if (form.accountType == AccountType.ENTREPRISE) {
        "Votre espace ${form.companyName} a été créé avec succès."
    } else {
        "Votre espace personnel a été créé avec succès."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FormulooBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SuccessCheckmarkIllustration()
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Votre espace est prêt !",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Bienvenue, ${form.firstName} !",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = FormulooOnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        FormulooButton(
            text = "Accéder à mon espace",
            onClick = onContinue,
        )
    }
}
