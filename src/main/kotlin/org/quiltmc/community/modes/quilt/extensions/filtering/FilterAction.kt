/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.filtering

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import kotlinx.serialization.Serializable

@Serializable
enum class FilterAction(
	val severity: Int,
	override val readableName: String,
	val validForUsers: Boolean = false
) : ChoiceEnum {
	RESPOND(-1, "Respond with the filter note"),
	DELETE(0, "Delete message"),
	TIMEOUT(1, "Timeout user"),
	KICK(2, "Kick user", true),
	BAN(3, "Ban user", true)
}
