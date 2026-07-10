plugins {
    id("formuloo.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.designsystem)
            implementation(projects.core.auth)
            implementation(projects.feature.hr)
            implementation(projects.feature.compta)
        }
    }
}
