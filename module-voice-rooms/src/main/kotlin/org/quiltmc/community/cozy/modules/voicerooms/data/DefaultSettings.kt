/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DataClassShouldBeImmutable")

package org.quiltmc.community.cozy.modules.voicerooms.data

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.storage.Data
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.EmbedBuilder
import org.quiltmc.community.cozy.modules.voicerooms.enabled

public data class DefaultSettings(
	public var name: String = "VC: {USERNAME}",
	public var userLimit: Int = 5,

	public var allowActivities: Boolean = true,
	public var allowSoundboard: Boolean = false,
	public var allowText: Boolean = true,
	public var allowVideo: Boolean = false,
	public var allowVoiceActivity: Boolean = true,

	public var allowListMode: Boolean = false,
	public val allowList: MutableSet<Snowflake> = mutableSetOf(),

	public val managers: MutableSet<Snowflake> = mutableSetOf(),
	public val muted: MutableSet<Snowflake> = mutableSetOf(),
) : Data

context(EmbedBuilder)
public fun DefaultSettings.apply() {
	title = "Default Configuration: Settings"
	color = DISCORD_BLURPLE

	description = buildString {
		appendLine("**Channel name:** `$name`")
		appendLine("**»** Placeholders: `{USERNAME}`, `{DISPLAY_NAME}`, `{TAG}`, `{DISCRIMINATOR}`")
		appendLine()

		append("**User limit:** ")

		if (userLimit > 0) {
			appendLine(userLimit)
		} else {
			appendLine("No limit")
		}

		appendLine()
		appendLine("**__Toggles__**")
		appendLine()
		appendLine("Activities: ${allowActivities.enabled}")
		appendLine("Allow-list mode: ${allowListMode.enabled}")
		appendLine("Soundboard: ${allowSoundboard.enabled}")
		appendLine("Text chat: ${allowText.enabled}")
		appendLine("Video/streaming: ${allowVideo.enabled}")
		appendLine("Voice activity: ${allowVoiceActivity.enabled}")
	}
}
