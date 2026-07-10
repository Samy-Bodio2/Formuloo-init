package com.formuloo.core.network

import com.formuloo.core.auth.TokenRepository
import com.formuloo.core.network.api.ComptaApiService
import com.formuloo.core.network.api.GesDocApiService
import com.formuloo.core.network.api.HrApiService
import com.formuloo.core.network.dto.auth.AuthApiService
import org.koin.dsl.module

val coreNetworkModule = module {
    single { createHttpClient(get<TokenRepository>()) }
    single { AuthApiService(get()) }
    single { HrApiService(get()) }
    single { ComptaApiService(get()) }
    single { GesDocApiService(get()) }
}
