/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.rolesync.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import dev.kord.common.entity.Snowflake

/**
 * The roles to sync between the two guilds.
 *
 * @param source The source role to sync. Modifications to this role will be synced to the target role.
 * @param target The target role that will be synced when the source role is changed.
 */
public data class RoleToSync(
	val source: Snowflake,
	val target: Snowflake,
)

/**
 * Class representing the configuration for the role sync module.
 *
 * Extend this class to implement your configuration loader. If you only need a basic config, then you can
 * take a look at [SimpleRoleSyncConfig] instead.
 */
public open class RoleSyncConfig {
	/**
	 * Override this to specify the roles that should be synced.
	 */
	public open suspend fun getRolesToSync(): List<RoleToSync> = listOf()

	/**
	 * Override this to provide a list of KordEx [Check]s that must pass for commands to be executed.
	 */
	public open suspend fun getCommandChecks(): List<Check<*>> = listOf()
}
