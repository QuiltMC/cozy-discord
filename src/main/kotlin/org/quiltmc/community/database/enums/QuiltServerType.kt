/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.enums

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import kotlinx.serialization.Serializable

@Serializable
enum class QuiltServerType(override val readableName: String) : ChoiceEnum {
    COMMUNITY("Community"),
    TOOLCHAIN("Toolchain")
}
