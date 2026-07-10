package com.formuloo.feature.hr

import com.formuloo.core.common.notification.NotificationService
import com.formuloo.core.common.notification.createNotificationService
import com.formuloo.core.common.sync.NetworkObserver
import com.formuloo.core.common.sync.createNetworkObserver
import com.formuloo.core.network.api.HrApiService
import com.formuloo.core.network.api.HrRemoteDataSource
import com.formuloo.feature.hr.data.repository.HrRepository
import com.formuloo.feature.hr.data.repository.HrRepositoryImpl
import com.formuloo.feature.hr.data.source.local.HrLocalDataSource
import com.formuloo.feature.hr.data.source.local.SqlDelightHrLocalDataSource
import com.formuloo.feature.hr.data.sync.LeaveSyncManager
import com.formuloo.feature.hr.presentation.viewmodel.ContractCreateViewModel
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeCreateViewModel
import com.formuloo.feature.hr.presentation.viewmodel.MyPresencesViewModel
import com.formuloo.feature.hr.presentation.viewmodel.PresencesAdminViewModel
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeDetailViewModel
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeEditViewModel
import com.formuloo.feature.hr.presentation.viewmodel.EmployeeListViewModel
import com.formuloo.feature.hr.presentation.viewmodel.HrDashboardViewModel
import com.formuloo.feature.hr.presentation.viewmodel.LeaveRequestViewModel
import com.formuloo.feature.hr.presentation.viewmodel.MyLeavesViewModel
import com.formuloo.feature.hr.presentation.viewmodel.OrgChartViewModel
import com.formuloo.feature.hr.presentation.viewmodel.DemandesDocumentRHViewModel
import com.formuloo.feature.hr.presentation.viewmodel.MesDemandesDocumentViewModel
import com.formuloo.feature.hr.presentation.viewmodel.PayrollAdminViewModel
import com.formuloo.feature.hr.presentation.viewmodel.SoldesCongesAdminViewModel
import com.formuloo.feature.hr.presentation.viewmodel.StatsRHViewModel
import com.formuloo.feature.hr.presentation.viewmodel.PayslipListViewModel
import com.formuloo.feature.hr.presentation.viewmodel.TeamApprovalViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val featureHrModule = module {
    single<HrRemoteDataSource> { get<HrApiService>() }
    single<HrLocalDataSource> { SqlDelightHrLocalDataSource(get()) }
    single<NetworkObserver> { createNetworkObserver() }
    single<NotificationService> { createNotificationService() }
    single<HrRepository> { HrRepositoryImpl(get(), get(), get()) }
    single { LeaveSyncManager(get(), get(), get()) }
    viewModelOf(::EmployeeListViewModel)
    viewModelOf(::EmployeeCreateViewModel)
    viewModel { (id: String) -> EmployeeDetailViewModel(get(), id) }
    viewModel { (id: String) -> EmployeeEditViewModel(get(), id) }
    viewModel { (id: String) -> ContractCreateViewModel(get(), id) }
    viewModelOf(::HrDashboardViewModel)
    viewModelOf(::LeaveRequestViewModel)
    viewModelOf(::MyLeavesViewModel)
    viewModelOf(::TeamApprovalViewModel)
    viewModelOf(::PayslipListViewModel)
    viewModelOf(::OrgChartViewModel)
    viewModelOf(::MyPresencesViewModel)
    viewModelOf(::PresencesAdminViewModel)
    viewModelOf(::PayrollAdminViewModel)
    viewModelOf(::MesDemandesDocumentViewModel)
    viewModelOf(::DemandesDocumentRHViewModel)
    viewModelOf(::SoldesCongesAdminViewModel)
    viewModelOf(::StatsRHViewModel)
}
