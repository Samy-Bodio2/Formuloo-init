package com.formuloo.core.auth

import org.koin.dsl.module

val coreAuthModule = module {
    single { TokenRepository() }
}
