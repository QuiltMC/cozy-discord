/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.github.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubSimpleUser(
	val email: String?,
	val id: Int,
	val login: String,
	val name: String?,
	val type: String,

	@SerialName("avatar_url")
	val avatarUrl: String,

	@SerialName("gravatar_id")
	val gravatarId: String?,

	@SerialName("site_admin")
	val isSiteAdmin: Boolean,

	@SerialName("html_url")
	val url: String,
)
