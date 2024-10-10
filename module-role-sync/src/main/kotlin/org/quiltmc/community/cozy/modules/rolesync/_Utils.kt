/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.rolesync

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Role
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.builders.ExtensionsBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import org.quiltmc.community.cozy.modules.rolesync.config.RoleSyncConfig
import org.quiltmc.community.cozy.modules.rolesync.config.SimpleRoleSyncConfig

@OptIn(ExperimentalCoroutinesApi::class)
public suspend fun getGuildForRoleSnowflake(roleId: Snowflake, bot: ExtensibleBot): Role =
	bot.getKoin().get<Kord>().guilds.flatMapConcat { it.roles }.first { it.id == roleId }

public fun ExtensionsBuilder.rolesync(config: RoleSyncConfig) {
	add { RoleSyncExtension(config) }
}

public fun ExtensionsBuilder.rolesync(body: SimpleRoleSyncConfig.Builder.() -> Unit): Unit =
	rolesync(SimpleRoleSyncConfig(body))
