package com.formuloo.convention

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin commune a tous les modules KMP "library" (core et feature).
 *
 * Applique : kotlin-multiplatform + android-library + kotlin-serialization
 * Targets  : androidTarget(), iosArm64(), iosSimulatorArm64(), jvm()
 * Android  : compileSdk/minSdk/targetSdk depuis le version catalog
 * commonMain : koin-core, kotlinx-coroutines-core, kotlinx-serialization-json
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.library")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                androidTarget {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }

                iosArm64()
                iosSimulatorArm64()

                jvm {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }

                sourceSets.getByName("commonMain") {
                    dependencies {
                        implementation(libs.findLibrary("koin-core").get())
                        implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                        implementation(libs.findLibrary("kotlinx-serialization-json").get())
                    }
                }
            }

            extensions.configure<LibraryExtension> {
                namespace = formulooNamespace
                compileSdk = libs.intVersion("android-compileSdk")

                defaultConfig {
                    minSdk = libs.intVersion("android-minSdk")
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
        }
    }
}
