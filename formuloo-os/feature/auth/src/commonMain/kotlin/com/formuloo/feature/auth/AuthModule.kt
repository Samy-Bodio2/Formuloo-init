package com.formuloo.feature.auth

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureAuthModule = module {
    viewModel { AuthViewModel(get(), get()) }
    viewModel { RegistrationViewModel(get()) }
    viewModel { OtpVerificationViewModel(get()) }
    viewModel { PasswordResetViewModel(get()) }
    viewModel { InvitationViewModel(get(), get()) }
}
