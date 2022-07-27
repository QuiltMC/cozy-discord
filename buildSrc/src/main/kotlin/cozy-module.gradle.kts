/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.cadixdev.gradle.licenser.LicenseExtension
import org.ec4j.gradle.EditorconfigCheckTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")

	id("com.expediagroup.graphql")
	id("com.google.devtools.ksp")
	id("io.gitlab.arturbosch.detekt")
	id("org.cadixdev.licenser")
	id("org.ec4j.editorconfig")
}

group = "org.quiltmc.community"
version = "1.0-SNAPSHOT"

repositories {
	mavenLocal()
	google()

	maven {
		name = "Kotlin Discord (Snapshots)"
		url = uri("https://maven.kotlindiscord.com/repository/maven-snapshots/")
	}

	maven {
		name = "Kotlin Discord (Public)"
		url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
	}

	maven {
		name = "Fabric"
		url = uri("https://maven.fabricmc.net")
	}

	maven {
		name = "QuiltMC (Releases)"
		url = uri("https://maven.quiltmc.org/repository/release/")
	}

	maven {
		name = "QuiltMC (Snapshots)"
		url = uri("https://maven.quiltmc.org/repository/snapshot/")
	}

	maven {
		name = "JitPack"
		url = uri("https://jitpack.io")
	}
}

configurations.all {
	resolutionStrategy.cacheDynamicVersionsFor(10, "seconds")
	resolutionStrategy.cacheChangingModulesFor(10, "seconds")
}

tasks {
	afterEvaluate {
		withType<KotlinCompile>().configureEach {
			kotlinOptions {
				jvmTarget = "17"
				freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
			}
		}

		license {
			header(rootDir.toPath().resolve("LICENSE"))
		}

		java {
			sourceCompatibility = JavaVersion.VERSION_17
			targetCompatibility = JavaVersion.VERSION_17
		}

		check {
			dependsOn("editorconfigCheck")
		}

		editorconfig {
			excludes = mutableListOf(
				"build",
				".*/**",
			)
		}
	}
}

detekt {
	buildUponDefaultConfig = true
	config = rootProject.files("detekt.yml")
}

// Credit to ZML for this workaround.
// https://github.com/CadixDev/licenser/issues/6#issuecomment-817048318
extensions.configure(LicenseExtension::class.java) {
	exclude {
		it.file.startsWith(buildDir)
	}
}

sourceSets {
	main {
		java {
			srcDir(file("$buildDir/generated/ksp/main/kotlin/"))
		}
	}

	test {
		java {
			srcDir(file("$buildDir/generated/ksp/test/kotlin/"))
		}
	}
}

val sourceJar = task("sourceJar", Jar::class) {
	dependsOn(tasks["classes"])
	archiveClassifier.set("sources")
	from(sourceSets.main.get().allSource)
}
