/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
	`kotlin-dsl`
}

repositories {
	google()
	gradlePluginPortal()
}

dependencies {
	implementation(gradleApi())
	implementation(localGroovy())

	implementation(kotlin("gradle-plugin", version = "1.8.0"))
	implementation(kotlin("serialization", version = "1.8.0"))

	implementation("gradle.plugin.org.cadixdev.gradle", "licenser", "0.6.1")
	implementation("com.github.jakemarsden", "git-hooks-gradle-plugin", "0.0.2")
	implementation("com.google.devtools.ksp", "com.google.devtools.ksp.gradle.plugin", "1.8.0-1.0.9")
	implementation("io.gitlab.arturbosch.detekt", "detekt-gradle-plugin", "1.22.0")
	implementation("org.ec4j.editorconfig", "org.ec4j.editorconfig.gradle.plugin", "0.0.3")

	implementation("com.expediagroup.graphql", "com.expediagroup.graphql.gradle.plugin", "6.3.5")
	implementation("com.github.johnrengelman.shadow", "com.github.johnrengelman.shadow.gradle.plugin", "7.1.2")
}
