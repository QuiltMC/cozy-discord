/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
	`maven-publish`
}

val sourceJar: Task by tasks.getting
//val javadocJar: Task by tasks.getting

publishing {
	repositories {
		maven {
			name = "QuiltMC"

			url = if (project.version.toString().contains("SNAPSHOT")) {
				uri(
					project.findProperty("maven.url.snapshots") as String?
						?: System.getenv("SNAPSHOTS_URL")
						?: "https://maven.quiltmc.org/repository/snapshots/"
				)
			} else {
				uri(
					project.findProperty("maven.url.releases") as String?
						?: System.getenv("MAVEN_URL")
						?: "https://maven.quiltmc.org/repository/releases/"
				)
			}

			credentials {
				username = project.findProperty("maven.user") as String?
					?: System.getenv("MAVEN_USERNAME")

				password = project.findProperty("maven.password") as String?
					?: System.getenv("MAVEN_PASSWORD")
			}

			version = project.version
		}
	}

	publications {
		create<MavenPublication>("maven") {
			from(components.getByName("java"))

			artifact(sourceJar)
//            artifact(javadocJar)
		}
	}
}
