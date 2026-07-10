package com.formuloo.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

class JvmDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val databaseFile = File(System.getProperty("user.home"), ".formuloo/formuloo.db")
        databaseFile.parentFile?.mkdirs()
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        if (!databaseFile.exists()) {
            FormulooDatabase.Schema.create(driver)
        }
        return driver
    }
}
