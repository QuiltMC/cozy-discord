/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.storage

import com.kotlindiscord.kord.extensions.storage.Data
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class BannedReactions(
	val categories: MutableSet<Snowflake> = mutableSetOf(),
	val reactionNames: MutableSet<String> = mutableSetOf(),
) : Data
