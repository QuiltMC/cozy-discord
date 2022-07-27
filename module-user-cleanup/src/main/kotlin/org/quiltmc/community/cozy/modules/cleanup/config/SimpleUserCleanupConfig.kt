/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.cleanup.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.lastOrNull
import org.quiltmc.community.cozy.modules.cleanup.GuildPredicate
import java.util.*
import kotlin.time.Duration

public class SimpleUserCleanupConfig(builder: Builder) : UserCleanupConfig() {
	override val dateFormattingLocale: Locale = builder.dateFormattingLocale
	override val registerCommand: Boolean = builder.registerCommand
	override val runAutomatically: Boolean = builder.runAutomatically

	private val taskDelay: Duration = builder.taskDelay!!
	private val maxPendingDuration: Duration = builder.maxPendingDuration!!
	private val loggingChannelName: String = builder.loggingChannelName!!

	private val guildPredicates: MutableList<GuildPredicate> = builder.guildPredicates
	private val commandChecks: MutableList<Check<*>> = builder.commandChecks

	init {
		guildPredicates.add { guild ->
			getLoggingChannelOrNull(guild) != null
		}
	}

	override suspend fun checkGuild(guild: Guild): Boolean =
		guildPredicates.all { it(guild) }

	override suspend fun getCommandChecks(): List<Check<*>> =
		commandChecks

	override suspend fun getLoggingChannelOrNull(guild: Guild): GuildMessageChannel? =
		guild.channels
			.filterIsInstance<GuildMessageChannel>()
			.filter { channel -> channel.name.equals(loggingChannelName, true) }
			.lastOrNull()

	override suspend fun getMaxPendingDuration(): Duration =
		maxPendingDuration

	override suspend fun getTaskDelay(): Duration =
		taskDelay

	public class Builder {
		public var dateFormattingLocale: Locale = Locale.UK
		public var taskDelay: Duration? = null
		public var maxPendingDuration: Duration? = null
		public var loggingChannelName: String? = null

		public var runAutomatically: Boolean = true
		public var registerCommand: Boolean = true

		internal val commandChecks: MutableList<Check<*>> = mutableListOf()
		internal val guildPredicates: MutableList<GuildPredicate> = mutableListOf()

		public fun validate() {
			if (taskDelay == null) {
				error("Required property not set: taskDelay")
			}

			if (maxPendingDuration == null) {
				error("Required property not set: maxPendingDuration")
			}

			if (loggingChannelName == null) {
				error("Required property not set: loggingChannelName")
			}
		}

		public fun commandCheck(body: Check<*>) {
			commandChecks.add(body)
		}

		public fun guildPredicate(body: GuildPredicate) {
			guildPredicates.add(body)
		}
	}
}

public inline fun SimpleUserCleanupConfig(body: SimpleUserCleanupConfig.Builder.() -> Unit): SimpleUserCleanupConfig {
	val builder = SimpleUserCleanupConfig.Builder()

	body(builder)

	return SimpleUserCleanupConfig(builder)
}
