package com.formuloo.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin pour le module :composeApp (entree Android + Desktop).
 *
 * Configure uniquement le bloc AGP `android {}` (ApplicationExtension).
 * Les plugins kotlin-multiplatform / compose-multiplatform / kotlin-compose
 * et la configuration des targets KMP (androidTarget(), jvm("desktop"))
 * sont appliques directement dans composeApp/build.gradle.kts, seul endroit
 * autorise a configurer le bloc kotlin {} pour ce module.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    namespace = "com.formuloo.os"
                    compileSdk = libs.intVersion("android-compileSdk")

                    defaultConfig {
                        applicationId = "com.formuloo.os"
                        minSdk = libs.intVersion("android-minSdk")
                        targetSdk = libs.intVersion("android-targetSdk")
                        versionCode = 1
                        versionName = "1.0"
                    }

                    compileOptions {
                        sourceCompatibility = JavaVersion.VERSION_17
                        targetCompatibility = JavaVersion.VERSION_17
                    }

                    packaging {
                        resources {
                            excludes += "/META-INF/{AL2.0,LGPL2.1}"
                        }
                    }
                }
            }
        }
    }
}
