/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.suggestions

import com.kotlindiscord.kord.extensions.*
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import dev.kord.common.Color
import kotlinx.serialization.Serializable

@Serializable
enum class SuggestionStatus(override val readableName: String, val color: Color) : ChoiceEnum {
    Open("Open", DISCORD_BLURPLE),

    Approved("Approved", DISCORD_FUCHSIA),

    Denied("Denied", DISCORD_RED),
    Invalid("Invalid", DISCORD_RED),
    Spam("Spam", DISCORD_RED),

    Future("Future Concern", DISCORD_YELLOW),
    Stale("Stale", DISCORD_YELLOW),

    Duplicate("Duplicate", DISCORD_BLACK),
    Implemented("Implemented", DISCORD_GREEN),
}
