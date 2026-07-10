plugins {
    id("formuloo.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.designsystem)
            implementation(projects.core.navigation)
            implementation(projects.core.auth)
            implementation(projects.core.network)
            implementation(libs.multiplatform.settings.no.arg)
        }
    }
}
