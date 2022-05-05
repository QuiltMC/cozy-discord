/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.misc

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthVersion(
    val id: String,

    @SerialName("author_id")
    val authorId: String,

    @SerialName("changelog_url")
    val changelogUrl: String?,

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

    val changelog: String,
    val downloads: Long,
    val dependencies: List<ModrinthVersionDependency>,
    val featured: Boolean,
    val files: List<ModrinthVersionFile>,
    val loaders: List<String>,
    val name: String,
)

@Serializable
data class ModrinthVersionDependency(
    @SerialName("dependency_type")
    val dependencyType: String,

    @SerialName("project_id")
    val projectId: String,

    @SerialName("version_id")
    val versionId: String?,
)

@Serializable
data class ModrinthVersionFile(
    val filename: String,
    val hashes: ModrinthVersionFileHash,
    val primary: Boolean,
    val url: String,
)

@Serializable
data class ModrinthVersionFileHash(
    val sha1: String,
    val sha512: String,
)
