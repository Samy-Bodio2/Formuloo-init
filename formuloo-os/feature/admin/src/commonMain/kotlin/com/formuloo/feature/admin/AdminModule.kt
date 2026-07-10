package com.formuloo.feature.admin

import com.formuloo.core.network.dto.auth.AuthApiService
import com.formuloo.feature.admin.data.repository.AdminRepository
import com.formuloo.feature.admin.data.repository.AdminRepositoryImpl
import com.formuloo.feature.admin.presentation.viewmodel.InviteUserViewModel
import com.formuloo.feature.admin.presentation.viewmodel.UsersViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureAdminModule = module {
    single<AdminRepository> { AdminRepositoryImpl(get<AuthApiService>()) }
    viewModel { UsersViewModel(get()) }
    viewModel { InviteUserViewModel(get()) }
}
