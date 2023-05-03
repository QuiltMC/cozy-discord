import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
	`cozy-module`
	`shadow-module`

	application

	id("com.github.jakemarsden.git-hooks")
}

allprojects {
	repositories {
		mavenLocal()
		mavenCentral()
		google()

		maven {
			name = "Sonatype Snapshots (Legacy)"
			url = uri("https://oss.sonatype.org/content/repositories/snapshots")
		}

		maven {
			name = "Sonatype Snapshots"
			url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
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
}

dependencies {
	implementation("io.ktor:ktor-client-encoding:2.2.4")
	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	ksp(libs.kordex.annotationProcessor)

	implementation(libs.excelkt)
	implementation(libs.kmongo)
	implementation(libs.rgxgen)

	implementation(libs.kordex.annotations)
	implementation(libs.kordex.core)
	implementation(libs.kordex.mappings)
	implementation(libs.kordex.phishing)
	implementation(libs.kordex.pluralkit)
	implementation(libs.kordex.unsafe)

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

	implementation(project(":module-log-parser"))
	implementation(project(":module-moderation"))
	implementation(project(":module-role-sync"))
	implementation(project(":module-tags"))
	implementation(project(":module-user-cleanup"))
	implementation(project(":module-welcome"))
}

graphql {
	client {
		schemaFile = rootProject.file("github.graphql")
//        sdlEndpoint = "https://docs.github.com/public/schema.docs.graphql"
		packageName = "quilt.ghgen"
		serializer = GraphQLSerializer.KOTLINX
	}
}

application {
	// This is deprecated, but the Shadow plugin requires it
	mainClassName = "org.quiltmc.community.AppKt"
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
		}
	}
}
