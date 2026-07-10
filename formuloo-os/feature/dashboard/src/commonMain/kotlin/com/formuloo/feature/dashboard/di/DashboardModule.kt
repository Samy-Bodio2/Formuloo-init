package com.formuloo.feature.dashboard.di

import com.formuloo.feature.dashboard.presentation.DashboardViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dashboardModule = module {
    viewModel { DashboardViewModel(get(), get()) }
}
