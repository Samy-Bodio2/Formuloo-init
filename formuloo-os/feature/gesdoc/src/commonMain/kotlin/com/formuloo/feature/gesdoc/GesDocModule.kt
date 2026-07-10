package com.formuloo.feature.gesdoc

import com.formuloo.core.network.api.GesDocApiService
import com.formuloo.core.network.api.GesDocRemoteDataSource
import com.formuloo.feature.gesdoc.data.repository.GesDocRepository
import com.formuloo.feature.gesdoc.data.repository.GesDocRepositoryImpl
import com.formuloo.feature.gesdoc.presentation.viewmodel.DocumentDetailViewModel
import com.formuloo.feature.gesdoc.presentation.viewmodel.DocumentOriginalViewModel
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocAuditViewModel
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocDashboardViewModel
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocExtractionViewModel
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocCertificationViewModel
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocUploadViewModel
import com.formuloo.feature.gesdoc.presentation.viewmodel.GesDocValidationViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val featureGesDocModule = module {
    single<GesDocRemoteDataSource> { get<GesDocApiService>() }
    single<GesDocRepository> { GesDocRepositoryImpl(get()) }
    viewModelOf(::GesDocDashboardViewModel)
    viewModelOf(::GesDocUploadViewModel)
    viewModelOf(::GesDocAuditViewModel)
    viewModel { (id: String) -> GesDocExtractionViewModel(get(), id) }
    viewModel { (id: String) -> GesDocValidationViewModel(get(), id) }
    viewModel { (id: String) -> GesDocCertificationViewModel(get(), id) }
    viewModel { (id: String) -> DocumentDetailViewModel(get(), id) }
    viewModel { (id: String) -> DocumentOriginalViewModel(get(), id) }
}
