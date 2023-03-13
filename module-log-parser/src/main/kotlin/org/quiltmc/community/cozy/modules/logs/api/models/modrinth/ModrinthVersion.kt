/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.api.models.modrinth

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ModrinthVersion(
	val id: String,

	@SerialName("date_published")
	val datePublished: Instant,

	@SerialName("game_versions")
	val gameVersions: List<String>,

	@SerialName("project_id")
	val projectId: String,

	@SerialName("version_number")
	val versionNumber: String,

	@SerialName("version_type")
	val versionType: String,

	val featured: Boolean,
	val files: List<ModrinthVersionFile>,
	val name: String,
)

@Serializable
public data class ModrinthVersionFile(
	val filename: String,
	val primary: Boolean,
	val url: String,
)
