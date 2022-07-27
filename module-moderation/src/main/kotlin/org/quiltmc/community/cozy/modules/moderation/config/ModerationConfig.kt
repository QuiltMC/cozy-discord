/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.moderation.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel

/**
 * Abstract class representing the configuration for the user moderation module.
 *
 * Extend this class to implement your configuration loader. If you only need a basic config, then you can
 * take a look at [SimpleModerationConfig] instead.
 */
public abstract class ModerationConfig {
	/**
	 * Override this to specify the channel where moderation operations should be logged.
	 * Return `null` if no channel could be found.
	 */
	public abstract suspend fun getLoggingChannelOrNull(guild: Guild): GuildMessageChannel?

	/**
	 * Override this to provide a list of KordEx [Check]s that must pass for moderation commands to be executed.
	 */
	public open suspend fun getCommandChecks(): List<Check<*>> = listOf()
}
