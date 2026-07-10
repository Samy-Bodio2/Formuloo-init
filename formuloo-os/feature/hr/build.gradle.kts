plugins {
    id("formuloo.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.network)
            implementation(projects.core.database)
            implementation(projects.core.designsystem)
            implementation(projects.core.auth)
            implementation(projects.core.navigation)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
