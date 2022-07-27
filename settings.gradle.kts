/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

pluginManagement {
	repositories {
		google()
		gradlePluginPortal()
	}
}

rootProject.name = "CozyDiscord"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
	versionCatalogs {
		create("libs") {
			from(files("libs.versions.toml"))
		}
	}
}

include(":module-moderation")
include(":module-role-sync")
include(":module-tags")
include(":module-user-cleanup")
include(":module-welcome")
