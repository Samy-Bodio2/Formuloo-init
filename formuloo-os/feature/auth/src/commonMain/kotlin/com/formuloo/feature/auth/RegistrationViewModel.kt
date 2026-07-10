package com.formuloo.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formuloo.core.common.NetworkResult
import com.formuloo.core.common.UiState
import com.formuloo.core.network.dto.auth.AuthApiService
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegistrationFormData(
    val accountType: AccountType = AccountType.PARTICULIER,
    val companyName: String = "",
    val sector: String = "",
    val companySize: String = "",
    val country: String = "",
    val city: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val secretQuestion: String = "",
    val secretAnswer: String = "",
)

enum class PasswordStrength { FAIBLE, MOYEN, FORT }

/** Reproduit le bareme de [com.formuloo.core.designsystem.PasswordStrengthIndicator]. */
fun passwordStrength(password: String): PasswordStrength {
    val criteria = listOf(
        password.length >= 12,
        password.any { it.isDigit() },
        password.any { it.isUpperCase() } && password.any { it.isLowerCase() },
        password.any { !it.isLetterOrDigit() },
    )
    val score = criteria.count { it }
    return when {
        score <= 1 -> PasswordStrength.FAIBLE
        score <= 3 -> PasswordStrength.MOYEN
        else -> PasswordStrength.FORT
    }
}

class RegistrationViewModel(private val authApiService: AuthApiService) : ViewModel() {

    private val _formData = MutableStateFlow(RegistrationFormData())
    val formData: StateFlow<RegistrationFormData> = _formData.asStateFlow()

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _fieldErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldErrors: StateFlow<Map<String, String>> = _fieldErrors.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<Unit>?>(null)
    val registerState: StateFlow<UiState<Unit>?> = _registerState.asStateFlow()

    /** Reinitialise le formulaire si le type de compte change (premiere composition). */
    fun setAccountType(accountType: AccountType) {
        if (_formData.value.accountType != accountType) {
            _formData.value = RegistrationFormData(accountType = accountType)
            _currentStep.value = 1
            _fieldErrors.value = emptyMap()
        }
    }

    fun updateForm(transform: (RegistrationFormData) -> RegistrationFormData) {
        _formData.value = transform(_formData.value)
    }

    fun setFieldErrors(errors: Map<String, String>) {
        _fieldErrors.value = errors
    }

    fun goToStep(step: Int) {
        _fieldErrors.value = emptyMap()
        _currentStep.value = step
    }

    fun nextStep(totalSteps: Int) {
        if (_currentStep.value < totalSteps) {
            goToStep(_currentStep.value + 1)
        }
    }

    /** Retourne true si l'etape precedente a ete atteinte, false si deja a l'etape 1. */
    fun previousStep(): Boolean {
        if (_currentStep.value > 1) {
            goToStep(_currentStep.value - 1)
            return true
        }
        return false
    }

    /**
     * Crée le compte côté backend (POST /register/). Le backend ne modélise que
     * des organisations (PME) : pour un compte PARTICULIER, on crée une organisation
     * "personnelle" nommée d'après l'utilisateur — invisible pour lui, juste un
     * conteneur technique. Réinitialise [registerState] à null après succès consommé
     * par l'appelant pour permettre de relancer l'inscription si besoin.
     */
    fun register() {
        val data = _formData.value
        val companyName = if (data.accountType == AccountType.PARTICULIER) {
            "Compte de ${data.firstName} ${data.lastName}".trim()
        } else {
            data.companyName
        }
        val slug = "${slugify(companyName)}-${Random.nextInt(1000, 9999)}"

        viewModelScope.launch {
            _registerState.value = UiState.Loading
            _registerState.value = when (
                val result = authApiService.register(
                    companyName = companyName,
                    companySlug = slug,
                    firstName = data.firstName,
                    lastName = data.lastName,
                    email = data.email,
                    password = data.password,
                    confirmPassword = data.passwordConfirm,
                )
            ) {
                is NetworkResult.Success -> UiState.Success(Unit)
                is NetworkResult.Error -> {
                    _fieldErrors.value = result.fieldErrors.associate { it.field to it.message }
                    UiState.Error(result.message, result.code)
                }
                is NetworkResult.Loading -> UiState.Loading
            }
        }
    }

    fun consumeRegisterState() {
        _registerState.value = null
    }
}

private fun slugify(name: String): String {
    val ascii = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    return ascii.ifBlank { "pme" }
}
