/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.voting

import com.kotlindiscord.kord.extensions.*
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import dev.kord.common.Color
import kotlinx.serialization.Serializable

@Serializable
enum class VoteStatus(override val readableName: String, val color: Color) : ChoiceEnum {
	Draft("Draft", DISCORD_BLACK),
	Open("Open", DISCORD_BLURPLE),

	Passed("Passed", DISCORD_GREEN),
	Failed("Failed", DISCORD_RED),
}
