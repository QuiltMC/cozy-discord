/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.rolesync.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import dev.kord.common.entity.Snowflake

public class SimpleRoleSyncConfig(builder: Builder) : RoleSyncConfig() {
	private val rolesToSync: MutableList<RoleToSync> = builder.rolesToSync
	private val commandChecks: MutableList<Check<*>> = builder.commandChecks

	override suspend fun getRolesToSync(): List<RoleToSync> =
		rolesToSync

	override suspend fun getCommandChecks(): List<Check<*>> =
		commandChecks

	public class Builder {
		internal var rolesToSync: MutableList<RoleToSync> = mutableListOf()
		internal val commandChecks: MutableList<Check<*>> = mutableListOf()

		public fun roleToSync(source: Snowflake, target: Snowflake) {
			rolesToSync.add(RoleToSync(source, target))
		}

		public fun commandCheck(body: Check<*>) {
			commandChecks.add(body)
		}
	}
}

public inline fun SimpleRoleSyncConfig(body: SimpleRoleSyncConfig.Builder.() -> Unit): SimpleRoleSyncConfig {
	val builder = SimpleRoleSyncConfig.Builder()

	body(builder)

	return SimpleRoleSyncConfig(builder)
}
