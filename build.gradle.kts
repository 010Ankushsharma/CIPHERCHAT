/*
 * CipherChat — Root Build Script
 * -----------------------------------------------------------------------
 * This file does NOT build any code directly. Its only job is to make
 * every plugin from the version catalog available to submodules, with
 * `apply false` so the plugin is resolved/downloaded once at the root
 * but only actually *applied* inside the modules that need it
 * (e.g. core:domain needs kotlinMultiplatform but not composeMultiplatform,
 * while feature:chat needs both).
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
}

// ---------------------------------------------------------------------
// Shared configuration applied to every module in the project, so we
// don't repeat boilerplate (repositories, Kotlin compiler opt-ins) in
// each module's own build.gradle.kts.
// ---------------------------------------------------------------------
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// ---------------------------------------------------------------------
// Convenience task: wipe every module's build output in one command.
// Useful during early development when module wiring changes a lot.
// ---------------------------------------------------------------------
tasks.register("cleanAll") {
    group = "build"
    description = "Deletes build directories for every module in the project."
    dependsOn(allprojects.map { it.tasks.matching { task -> task.name == "clean" } })
}
