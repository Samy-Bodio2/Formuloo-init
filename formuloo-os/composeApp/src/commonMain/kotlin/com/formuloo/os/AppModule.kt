package com.formuloo.os

import com.formuloo.core.auth.coreAuthModule
import com.formuloo.core.database.coreDatabaseModule
import com.formuloo.core.network.coreNetworkModule
import com.formuloo.feature.admin.featureAdminModule
import com.formuloo.feature.analytics.featureAnalyticsModule
import com.formuloo.feature.auth.featureAuthModule
import com.formuloo.feature.compta.featureComptaModule
import com.formuloo.feature.crm.featureCrmModule
import com.formuloo.feature.dashboard.di.dashboardModule
import com.formuloo.feature.gesdoc.featureGesDocModule
import com.formuloo.feature.hr.featureHrModule
import com.formuloo.feature.projects.featureProjectsModule
import com.formuloo.feature.stock.featureStockModule
import org.koin.core.module.Module

/** Fournit les dependances specifiques a la plateforme (ex: DatabaseDriverFactory). */
expect val platformModule: Module

val appModules: List<Module> = listOf(
    platformModule,
    coreAuthModule,
    coreNetworkModule,
    coreDatabaseModule,
    featureAuthModule,
    featureAdminModule,
    featureHrModule,
    featureComptaModule,
    featureGesDocModule,
    featureCrmModule,
    featureStockModule,
    featureProjectsModule,
    featureAnalyticsModule,
    dashboardModule,
)
