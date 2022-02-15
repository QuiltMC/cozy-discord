/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.moderation

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import org.quiltmc.community.cozy.modules.moderation.config.ModerationConfig
import org.quiltmc.community.cozy.modules.moderation.config.SimpleModerationConfig

public fun ExtensibleBotBuilder.ExtensionsBuilder.moderation(config: ModerationConfig) {
    add { ModerationExtension(config) }
}

public fun ExtensibleBotBuilder.ExtensionsBuilder.moderation(body: SimpleModerationConfig.Builder.() -> Unit): Unit =
    moderation(SimpleModerationConfig(body))
