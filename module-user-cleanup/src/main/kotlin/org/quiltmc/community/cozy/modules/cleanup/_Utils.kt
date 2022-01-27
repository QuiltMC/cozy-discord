/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.cleanup

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import org.quiltmc.community.cozy.modules.cleanup.config.SimpleUserCleanupConfig
import org.quiltmc.community.cozy.modules.cleanup.config.UserCleanupConfig

public fun ExtensibleBotBuilder.ExtensionsBuilder.userCleanup(config: UserCleanupConfig) {
    add { UserCleanupExtension(config) }
}

public fun ExtensibleBotBuilder.ExtensionsBuilder.userCleanup(body: SimpleUserCleanupConfig.Builder.() -> Unit): Unit =
    userCleanup(SimpleUserCleanupConfig(body))
