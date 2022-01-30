/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.cleanup.config

import com.kotlindiscord.kord.extensions.checks.types.Check
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import java.util.*
import kotlin.time.Duration

/**
 * Abstract class representing the configuration for the user cleanup plugin.
 *
 * Extend this class to implement your configuration loader. If you only need a basic config, then you can
 * take a look at [SimpleUserCleanupConfig] instead.
 */
public abstract class UserCleanupConfig {
    /** Locale to use for formatting user join dates. **/
    public abstract val dateFormattingLocale: Locale

    /** Whether to run user cleanups automatically via scheduled task. **/
    public abstract val runAutomatically: Boolean

    /** Whether to register a slash command for running cleanups manually. **/
    public abstract val registerCommand: Boolean

    /**
     * Override this to specify the channel where cleanup operations should be logged, given the guild being
     * cleaned up. Return `null` if no channel could be found.
     */
    public abstract suspend fun getLoggingChannelOrNull(guild: Guild): GuildMessageChannel?

    /** Override this to supply a [Duration] representing how long to wait before kicking a pending user. **/
    public abstract suspend fun getMaxPendingDuration(): Duration

    /** Override this to supply a [Duration] representing the time between task runs. **/
    public abstract suspend fun getTaskDelay(): Duration

    /**
     * Wrapping function for [getLoggingChannelOrNull], with a not-null assertion.
     */
    public open suspend fun getLoggingChannel(guild: Guild): GuildMessageChannel =
        getLoggingChannelOrNull(guild)!!

    /**
     * Override this to provide a check against the given guild, returning `true` if cleanup operations should run
     * on the given guild, or `false` otherwise.
     */
    public open suspend fun checkGuild(guild: Guild): Boolean = true

    /**
     * Override this to provide a list of KordEx [Check]s that must pass for the cleanup command to be executed.
     */
    public open suspend fun getCommandChecks(): List<Check<*>> = listOf()
}
