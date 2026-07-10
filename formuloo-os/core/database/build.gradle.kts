plugins {
    id("formuloo.kmp.library")
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jvm)
        }
    }
}

sqldelight {
    databases {
        create("FormulooDatabase") {
            packageName.set("com.formuloo.core.database")
        }
    }
}
