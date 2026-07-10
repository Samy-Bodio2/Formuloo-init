rootProject.name = "Formuloo-os"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Entry points
include(":composeApp")

// Core modules
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:designsystem")
include(":core:auth")
include(":core:navigation")

// Feature modules
include(":feature:auth")
include(":feature:admin")
include(":feature:hr")
include(":feature:compta")
include(":feature:crm")
include(":feature:stock")
include(":feature:projects")
include(":feature:analytics")
include(":feature:gesdoc")
include(":feature:dashboard")
