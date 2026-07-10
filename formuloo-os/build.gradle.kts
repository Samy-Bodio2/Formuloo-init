// Fichier de build racine.
// Les plugins KMP/Android/Compose sont appliques par module via les
// convention plugins de build-logic (formuloo.kmp.library, formuloo.kmp.feature,
// formuloo.android.application). Ils sont neanmoins declares ici avec
// "apply false" pour que leurs classes (ApplicationExtension,
// KotlinMultiplatformExtension, ComposeExtension, ...) soient resolues sur le
// classpath de plugins partage avec build-logic (sinon "Could not generate a
// decorated class" lors de l'application des convention plugins).

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
}
