import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
	`cozy-module`
	`shadow-module`

	id("com.github.jakemarsden.git-hooks")
}

allprojects {
	repositories {
		mavenLocal()
	}
}

dependencies {
	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	implementation(libs.excelkt)
	implementation(libs.kmongo)
	implementation(libs.rgxgen)

	implementation(libs.ktor.client.encoding)

	implementation(libs.commons.text)
	implementation(libs.homoglyph)
	implementation(libs.jansi)
	implementation(libs.jsoup)
	implementation(libs.semver)

	implementation(libs.logback)
	implementation(libs.logback.groovy)
	implementation(libs.logging)
	implementation(libs.groovy)

	implementation(platform(libs.kotlin.bom))
	implementation(libs.kotlin.stdlib)
	implementation(libs.kx.ser)
	implementation(libs.graphql)

	implementation(projects.moduleAma)
	implementation(projects.moduleLogParser)
	implementation(projects.moduleModeration)
	implementation(projects.moduleRoleSync)
}

kordEx {
	bot {
		mainClass = "org.quiltmc.community.AppKt"
	}

	module("annotations")
	module("extra-mappings")
	module("extra-phishing")
	module("extra-pluralkit")
	module("extra-tags")
	module("extra-welcome")
	module("unsafe")
}

graphql {
	client {
		schemaFile = rootProject.file("github.graphql")
//        sdlEndpoint = "https://docs.github.com/public/schema.docs.graphql"
		packageName = "quilt.ghgen"
		serializer = GraphQLSerializer.KOTLINX
	}
}

gitHooks {
	setHooks(
		mapOf("pre-commit" to "updateLicense detekt")
	)
}

tasks {
	jar {
		manifest {
			attributes(
				"Main-Class" to "org.quiltmc.community.AppKt"
			)

			if (System.getenv("GITHUB_SHA") != null) {
				attributes(
					"Implementation-Version" to System.getenv("GITHUB_SHA")
				)
			}
		}
	}
}
