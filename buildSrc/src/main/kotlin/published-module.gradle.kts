/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.gradle.api.publish.maven.MavenPublication

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
                uri("https://maven.quiltmc.org/repository/snapshots/")
            } else {
                uri("https://maven.quiltmc.org/repository/releases/")
            }

            credentials {
                username = project.findProperty("maven.user") as String?
                    ?: System.getenv("MAVEN_USER")

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
