plugins {
    id("formuloo.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(libs.multiplatform.settings.no.arg)
        }
    }
}
