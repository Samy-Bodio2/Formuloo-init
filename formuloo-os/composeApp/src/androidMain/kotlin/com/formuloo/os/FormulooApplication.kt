package com.formuloo.os

import android.app.Application
import com.formuloo.core.common.sync.AndroidAppContextHolder
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FormulooApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContextHolder.appContext = this
        startKoin {
            androidContext(this@FormulooApplication)
            modules(appModules)
        }
    }
}
