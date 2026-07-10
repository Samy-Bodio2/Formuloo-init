package com.formuloo.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin pour les modules KMP "feature" (UI Compose Multiplatform).
 *
 * Etend : KmpLibraryConventionPlugin
 * Ajoute : kotlin-compose (compilateur Compose) + Compose Multiplatform
 *          (runtime/foundation/material3/ui/resources), navigation-compose,
 *          koin-compose(-viewmodel), androidx-lifecycle-viewmodel(-compose),
 *          androidx-lifecycle-runtime-compose
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpLibraryConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val compose = ComposePlugin.Dependencies(target)

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain") {
                    dependencies {
                        implementation(compose.runtime)
                        implementation(compose.foundation)
                        implementation(compose.material3)
                        implementation(compose.materialIconsExtended)
                        implementation(compose.ui)
                        implementation(compose.components.resources)

                        implementation(libs.findLibrary("navigation-compose").get())
                        implementation(libs.findLibrary("koin-compose").get())
                        implementation(libs.findLibrary("koin-compose-viewmodel").get())
                        implementation(libs.findLibrary("androidx-lifecycle-viewmodel").get())
                        implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                        implementation(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                    }
                }
            }
        }
    }
}
