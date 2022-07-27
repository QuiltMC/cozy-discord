/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.filtering

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import kotlinx.serialization.Serializable

@Serializable
enum class MatchType(override val readableName: String) : ChoiceEnum {
	CONTAINS("Target partially contains this text"),
	EXACT("Target exactly matches this text"),
	REGEX("Target exactly matches this regular expression"),
	REGEX_CONTAINS("Target contains this regular expression"),
	INVITE("Target contains an invite for this guild ID"),
}
