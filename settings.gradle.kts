pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "1.5.30"
        kotlin("plugin.serialization") version "1.5.30"

        id("com.google.devtools.ksp") version "1.5.30-1.0.0-beta08"
        id("com.github.jakemarsden.git-hooks") version "0.0.1"
        id("com.github.johnrengelman.shadow") version "5.2.0"
        id("io.gitlab.arturbosch.detekt") version "1.17.1"
        id("com.expediagroup.graphql") version "5.2.0"
    }
}

rootProject.name = "CozyDiscord"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}
