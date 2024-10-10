/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
	`api-module`
	`cozy-module`
	`published-module`
}

dependencies {
	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	implementation(libs.ktor.client.cio)

	implementation(libs.autolink)
	implementation(libs.flexver)
	implementation(libs.jsoup)
	implementation(libs.kaml)
	implementation(libs.logging)
	implementation(libs.semver)

	implementation(platform(libs.kotlin.bom))
	implementation(libs.kotlin.stdlib)
}

kordEx {
	module("pluralkit")
	module("dev-unsafe")
}
