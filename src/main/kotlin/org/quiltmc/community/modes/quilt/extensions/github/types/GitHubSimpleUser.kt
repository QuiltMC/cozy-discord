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
	val id: Int,
	val login: String,
	val type: String,

	val email: String? = null,
	val name: String? = null,

	@SerialName("avatar_url")
	val avatarUrl: String,

	@SerialName("gravatar_id")
	val gravatarId: String? = null,

	@SerialName("site_admin")
	val isSiteAdmin: Boolean,

	@SerialName("html_url")
	val url: String,
)
