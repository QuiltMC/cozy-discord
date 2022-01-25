/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.filtering

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import kotlinx.serialization.Serializable

@Serializable
enum class FilterAction(val severity: Int, override val readableName: String) : ChoiceEnum {
    DELETE(0, "Delete message"),
    KICK(1, "Delete message and kick user"),
    BAN(2, "Delete message and ban user")
}
