/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.voicerooms.data

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.storage.Data
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.EmbedBuilder

public data class GuildConfig(
	val archiveCategory: Snowflake,
	val lobbyChannel: Snowflake,
	val loggingChannel: Snowflake,
	val targetCategory: Snowflake,

	val moderatorRoles: MutableSet<Snowflake> = mutableSetOf()
) : Data

context(EmbedBuilder)
public fun GuildConfig.apply() {
	title = "Voice Rooms: Server Settings"
	color = DISCORD_BLURPLE

	description = buildString {
		appendLine("**Archive Category:** <#$archiveCategory>")
		appendLine("**Logging channel:** <#$loggingChannel>")
		appendLine("**Parent voice channel:** <#$lobbyChannel>")
		appendLine()
		appendLine("**__Moderator Roles__**")
		appendLine()

		if (moderatorRoles.isEmpty()) {
			appendLine("No roles configured.")
		} else {
			moderatorRoles.forEach {
				appendLine("**»** <@&$it>")
			}
		}
	}
}
