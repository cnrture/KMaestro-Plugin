rootProject.name = "KMaestro"

pluginManagement {
    plugins {
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    }
}