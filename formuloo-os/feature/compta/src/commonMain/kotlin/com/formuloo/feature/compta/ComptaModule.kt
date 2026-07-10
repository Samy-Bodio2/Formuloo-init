package com.formuloo.feature.compta

import com.formuloo.core.network.api.ComptaApiService
import com.formuloo.core.network.api.ComptaRemoteDataSource
import com.formuloo.feature.compta.data.repository.ComptaRepository
import com.formuloo.feature.compta.data.repository.ComptaRepositoryImpl
import com.formuloo.feature.compta.presentation.viewmodel.ComptaDashboardViewModel
import com.formuloo.feature.compta.presentation.viewmodel.ComptaPlanViewModel
import com.formuloo.feature.compta.presentation.viewmodel.InvoiceCreateViewModel
import com.formuloo.feature.compta.presentation.viewmodel.InvoiceDetailViewModel
import com.formuloo.feature.compta.presentation.viewmodel.InvoiceListViewModel
import com.formuloo.feature.compta.presentation.viewmodel.PaymentListViewModel
import com.formuloo.feature.compta.presentation.viewmodel.PurchaseInvoiceDetailViewModel
import com.formuloo.feature.compta.presentation.viewmodel.PurchaseInvoiceListViewModel
import com.formuloo.feature.compta.presentation.viewmodel.BalanceViewModel
import com.formuloo.feature.compta.presentation.viewmodel.DeclarationTVAViewModel
import com.formuloo.feature.compta.presentation.viewmodel.EtatsFinanciersViewModel
import com.formuloo.feature.compta.presentation.viewmodel.ExercicesViewModel
import com.formuloo.feature.compta.presentation.viewmodel.JournauxViewModel
import com.formuloo.feature.compta.presentation.viewmodel.AvoirsViewModel
import com.formuloo.feature.compta.presentation.viewmodel.EcrituresListViewModel
import com.formuloo.feature.compta.presentation.viewmodel.ImmobilisationsViewModel
import com.formuloo.feature.compta.presentation.viewmodel.SaisieViewModel
import com.formuloo.feature.compta.presentation.viewmodel.SupplierPaymentListViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val featureComptaModule = module {
    single<ComptaRemoteDataSource> { get<ComptaApiService>() }
    single<ComptaRepository> { ComptaRepositoryImpl(get()) }
    viewModelOf(::ComptaDashboardViewModel)
    viewModelOf(::ComptaPlanViewModel)
    viewModelOf(::SaisieViewModel)
    viewModelOf(::JournauxViewModel)
    viewModelOf(::EcrituresListViewModel)
    viewModelOf(::ExercicesViewModel)
    viewModelOf(::EtatsFinanciersViewModel)
    viewModelOf(::BalanceViewModel)
    viewModelOf(::DeclarationTVAViewModel)
    viewModelOf(::ImmobilisationsViewModel)
    viewModelOf(::InvoiceListViewModel)
    viewModel { (id: Int) -> InvoiceDetailViewModel(get(), id) }
    viewModelOf(::InvoiceCreateViewModel)
    viewModelOf(::PurchaseInvoiceListViewModel)
    viewModel { (id: Int) -> PurchaseInvoiceDetailViewModel(get(), id) }
    viewModelOf(::PaymentListViewModel)
    viewModelOf(::SupplierPaymentListViewModel)
    viewModelOf(::AvoirsViewModel)
}
