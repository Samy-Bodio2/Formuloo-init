plugins {
    id("formuloo.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
        }
    }
}
