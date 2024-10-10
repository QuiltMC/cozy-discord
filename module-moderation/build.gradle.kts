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

	implementation(libs.logging)

	implementation(platform(libs.kotlin.bom))
	implementation(libs.kotlin.stdlib)
}

kordEx {
	module("pluralkit")

	plugin {
		pluginClass = "org.quiltmc.community.cozy.modules.moderation.ModerationPlugin"
		id = "quiltmc-moderation"
		version = "1.0.1"

		author = "QuiltMC"
		description = "Various moderation tools for the QuiltMC community."
		license = "MPL-2.0"
	}
}
