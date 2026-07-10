package com.formuloo.feature.auth

import com.russhwolf.settings.Settings

/**
 * Marqueur persistant "onboarding deja vu", utilise par le SplashScreen pour
 * decider de naviguer vers l'onboarding (premiere ouverture) ou directement
 * vers Login/Home.
 */
object OnboardingPreferences {
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private val settings: Settings = Settings()

    fun isOnboardingDone(): Boolean = settings.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone() {
        settings.putBoolean(KEY_ONBOARDING_DONE, true)
    }
}
