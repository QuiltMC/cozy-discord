import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application

    kotlin("jvm") version "1.5.10"
    kotlin("plugin.serialization") version "1.5.10"

    id("com.github.jakemarsden.git-hooks") version "0.0.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.15.0"
}

group = "org.quiltmc.community"
version = "1.0-SNAPSHOT"

repositories {
    // You can remove this if you're not testing locally-installed KordEx builds
    mavenLocal()

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }

    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }

    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.15.0")

//    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.4.0-20210527.122057-237")
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.4.1-SNAPSHOT")
    implementation("com.kotlindiscord.kord.extensions:extra-mappings:1.4.1-SNAPSHOT")

    implementation("org.apache.commons:commons-text:1.9")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:2.0.3")
    implementation("org.codehaus.groovy:groovy:3.0.8")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
}

configurations.all {
    resolutionStrategy.cacheDynamicVersionsFor(10, "seconds")
    resolutionStrategy.cacheChangingModulesFor(10, "seconds")
}

application {
    // This is deprecated, but the Shadow plugin requires it
    mainClassName = "org.quiltmc.community.AppKt"
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "detekt")
    )
}

// If you don't want the import, remove it and use org.jetbrains.kotlin.gradle.tasks.KotlinCompile
tasks.withType<KotlinCompile> {
    // Current LTS version of Java
    kotlinOptions.jvmTarget = "15"

    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    kotlinOptions.useIR = true
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.quiltmc.community.AppKt"
        )
    }
}

java {
    // Current LTS version of Java
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

detekt {
    buildUponDefaultConfig = true
    config = rootProject.files("detekt.yml")
}
