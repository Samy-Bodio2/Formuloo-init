package com.formuloo.core.database

import org.koin.dsl.module

val coreDatabaseModule = module {
    single { FormulooDatabase(get<DatabaseDriverFactory>().createDriver()) }
}
