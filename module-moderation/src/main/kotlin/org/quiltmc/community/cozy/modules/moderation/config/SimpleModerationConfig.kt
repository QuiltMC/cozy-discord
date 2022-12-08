/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.moderation.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.GuildMessageChannel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull

public class SimpleModerationConfig(builder: Builder) : ModerationConfig() {
	private val loggingChannelName: String = builder.loggingChannelName!!
	private val verifiedRoleName: String = builder.verifiedRoleName!!
	private val commandChecks: MutableList<Check<*>> = builder.commandChecks

	override suspend fun getLoggingChannelOrNull(guild: Guild): GuildMessageChannel? =
		guild.channels
			.filterIsInstance<GuildMessageChannel>()
			.filter { channel -> channel.name.equals(loggingChannelName, true) }
			.lastOrNull()

	override suspend fun getVerifiedRole(guild: Guild): Role =
		guild.roles.filter { role -> role.name.equals(verifiedRoleName, true) }
			.last()

	override suspend fun getCommandChecks(): List<Check<*>> =
		commandChecks

	public class Builder {
		public var loggingChannelName: String? = null
		public var verifiedRoleName: String? = null

		internal val commandChecks: MutableList<Check<*>> = mutableListOf()

		public fun commandCheck(body: Check<*>) {
			commandChecks.add(body)
		}
	}
}

public inline fun SimpleModerationConfig(body: SimpleModerationConfig.Builder.() -> Unit): SimpleModerationConfig {
	val builder = SimpleModerationConfig.Builder()

	body(builder)

	return SimpleModerationConfig(builder)
}
