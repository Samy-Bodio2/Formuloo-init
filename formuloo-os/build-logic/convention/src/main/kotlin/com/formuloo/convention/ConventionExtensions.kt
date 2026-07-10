package com.formuloo.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Acces au version catalog `libs` (gradle/libs.versions.toml) depuis les convention plugins. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Derive un namespace Android unique a partir du chemin Gradle, ex: `:feature:hr` -> `com.formuloo.feature.hr`. */
internal val Project.formulooNamespace: String
    get() = "com.formuloo." + path.removePrefix(":").replace(":", ".").replace("-", "")

internal fun VersionCatalog.intVersion(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()
