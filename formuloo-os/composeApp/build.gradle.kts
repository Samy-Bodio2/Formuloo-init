import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("formuloo.android.application")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.designsystem)
            implementation(projects.core.navigation)
            implementation(projects.core.auth)
            implementation(projects.core.network)
            implementation(projects.core.database)
            implementation(projects.feature.auth)
            implementation(projects.feature.admin)
            implementation(projects.feature.hr)
            implementation(projects.feature.compta)
            implementation(projects.feature.crm)
            implementation(projects.feature.stock)
            implementation(projects.feature.projects)
            implementation(projects.feature.analytics)
            implementation(projects.feature.gesdoc)
            implementation(projects.feature.dashboard)

            implementation(libs.kotlinx.serialization.json)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.navigation.compose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.formuloo.os.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Formuloo-os"
            packageVersion = "1.0.0"
        }
    }
}
