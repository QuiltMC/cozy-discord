/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.rolesync

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Role
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import org.quiltmc.community.cozy.modules.rolesync.config.RoleSyncConfig
import org.quiltmc.community.cozy.modules.rolesync.config.SimpleRoleSyncConfig

@OptIn(FlowPreview::class)
public suspend fun getGuildForRoleSnowflake(roleId: Snowflake, bot: ExtensibleBot): Role =
    bot.getKoin().get<Kord>().guilds.flatMapConcat { it.roles }.first { it.id == roleId }

public fun ExtensibleBotBuilder.ExtensionsBuilder.rolesync(config: RoleSyncConfig) {
    add { RoleSyncExtension(config) }
}

public fun ExtensibleBotBuilder.ExtensionsBuilder.rolesync(body: SimpleRoleSyncConfig.Builder.() -> Unit): Unit =
    rolesync(SimpleRoleSyncConfig(body))
