import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
	id("com.github.johnrengelman.shadow")
}

tasks.withType<ShadowJar>() {
	isZip64 = true
}
