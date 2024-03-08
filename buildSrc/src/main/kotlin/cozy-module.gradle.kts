/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.cadixdev.gradle.licenser.LicenseExtension
//import org.ec4j.gradle.EditorconfigCheckTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")

	id("com.expediagroup.graphql")
	id("com.google.devtools.ksp")
	id("io.gitlab.arturbosch.detekt")
	id("org.cadixdev.licenser")
//	id("org.ec4j.editorconfig")
}

group = "org.quiltmc.community"
version = "1.0.1-SNAPSHOT"

repositories {
	mavenLocal()
	google()

	maven {
		name = "Sleeping Town"
		url = uri("https://repo.sleeping.town")

		content {
			includeGroup("com.unascribed")
		}
	}

	maven {
		name = "Sonatype Snapshots"
		url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
	}

	maven {
		name = "Sonatype Snapshots (Legacy)"
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
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
		name = "Shedaniel"
		url = uri("https://maven.shedaniel.me")
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

configurations.all {
	resolutionStrategy.cacheDynamicVersionsFor(10, "seconds")
	resolutionStrategy.cacheChangingModulesFor(10, "seconds")
}

tasks {
	afterEvaluate {
		withType<KotlinCompile>().configureEach {
			kotlinOptions {
				jvmTarget = "17"
				freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
			}
		}

		license {
			header(rootDir.toPath().resolve("LICENSE"))
		}

		java {
			sourceCompatibility = JavaVersion.VERSION_17
			targetCompatibility = JavaVersion.VERSION_17
		}

//		check {
//			dependsOn("editorconfigCheck")
//		}
//
//		editorconfig {
//			excludes = mutableListOf(
//				"build",
//				".*/**",
//			)
//		}
	}
}

detekt {
	buildUponDefaultConfig = true
	config.from(rootProject.files("detekt.yml"))
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
