package com.formuloo.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(FormulooDatabase.Schema, "formuloo.db")
}
