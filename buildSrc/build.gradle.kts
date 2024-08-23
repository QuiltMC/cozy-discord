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

	implementation(kotlin("gradle-plugin", version = "2.0.20"))
	implementation(kotlin("serialization", version = "2.0.20"))

	implementation("com.github.jakemarsden", "git-hooks-gradle-plugin", "0.0.2")
	implementation("com.expediagroup.graphql", "com.expediagroup.graphql.gradle.plugin", "7.1.4")
	implementation("com.github.johnrengelman.shadow", "com.github.johnrengelman.shadow.gradle.plugin", "8.1.1")
	implementation("com.google.devtools.ksp", "com.google.devtools.ksp.gradle.plugin", "2.0.20-1.0.24")
	implementation("dev.kordex.gradle.plugins", "kordex", "1.4.1")
	implementation("dev.yumi", "yumi-gradle-licenser", "1.2.0")
	implementation("io.gitlab.arturbosch.detekt", "detekt-gradle-plugin", "1.23.6")
}
