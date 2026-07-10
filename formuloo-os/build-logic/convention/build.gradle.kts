import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.formuloo.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.serialization.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "formuloo.kmp.library"
            implementationClass = "com.formuloo.convention.KmpLibraryConventionPlugin"
        }
        register("kmpFeature") {
            id = "formuloo.kmp.feature"
            implementationClass = "com.formuloo.convention.KmpFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "formuloo.android.application"
            implementationClass = "com.formuloo.convention.AndroidApplicationConventionPlugin"
        }
    }
}
