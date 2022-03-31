/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.annotations.DoNotChain
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.isNotInThread
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.duration
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalDuration
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.removeTimeout
import com.kotlindiscord.kord.extensions.utils.timeout
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import kotlinx.datetime.DateTimePeriod
import org.quiltmc.community.cozy.modules.moderation.config.ModerationConfig

public val MAXIMUM_SLOWMODE_DURATION: DateTimePeriod = DateTimePeriod(hours = 6)
public const val MAX_TIMEOUT_SECS: Int = 60 * 60 * 24 * 28

/**
 * Moderation, extension, provides different moderation related tools.
 *
 * Currently includes:
 * - Slowmode command
 */
public class ModerationExtension(
    private val config: ModerationConfig
) : Extension() {
    override val name: String = ModerationPlugin.id

    @OptIn(DoNotChain::class)
    override suspend fun setup() {
        ephemeralSlashCommand(::TimeoutArguments) {
            name = "timeout"
            description = "Remove or apply a timeout to a user"

            config.getCommandChecks().forEach(::check)

            action {
                if (arguments.duration != null) {
                    arguments.user.timeout(
                        arguments.duration!!,
                        reason = "Timed out by ${user.asUser().tag}"
                    )
                } else {
                    arguments.user.removeTimeout(
                        "Timeout removed by ${user.asUser().tag}"
                    )
                }
            }
        }

        ephemeralSlashCommand {
            name = "slowmode"
            description = "Manage slowmode of the current channel"

            check { anyGuild() }
            check { isNotInThread() }

            config.getCommandChecks().forEach(::check)

            ephemeralSubCommand {
                name = "get"
                description = "Get the slowmode of the channel"

                action {
                    respond {
                        content = "Slowmode is currently " +
                                "${channel.asChannelOf<TextChannel>().userRateLimit} second(s)."
                    }
                }
            }

            ephemeralSubCommand {
                name = "reset"
                description = "Reset the slowmode of the channel back to 0"

                action {
                    val channel = channel.asChannel() as TextChannel

                    channel.edit {
                        rateLimitPerUser = 0
                    }

                    respond {
                        content = "Slowmode reset."
                    }
                }
            }

            ephemeralSubCommand(::SlowmodeEditArguments) {
                name = "set"
                description = "Set the slowmode of the channel"

                action {
                    val channel = channel.asChannel() as TextChannel

                    channel.edit {
                        rateLimitPerUser = arguments.duration.toTotalSeconds()
                    }

                    config.getLoggingChannelOrNull(guild!!.asGuild())?.createEmbed {
                        title = "Slowmode changed"
                        description = "Set to ${arguments.duration.toTotalSeconds()} second(s)."
                        color = DISCORD_BLURPLE

                        field {
                            inline = true
                            name = "Channel"
                            value = channel.mention
                        }

                        field {
                            inline = true
                            name = "User"
                            value = user.mention
                        }
                    }

                    respond {
                        content = "Slowmode set to ${arguments.duration.toTotalSeconds()} second(s)."
                    }
                }
            }
        }
    }

    public inner class SlowmodeEditArguments : Arguments() {
        public val duration: DateTimePeriod by duration {
            name = "duration"
            description = "The new duration of the slowmode"

            validate {
                failIf(
                    "Slowmode cannot be longer than ${MAXIMUM_SLOWMODE_DURATION.hours} hours"
                ) { value > MAXIMUM_SLOWMODE_DURATION }
            }
        }
    }

    public inner class TimeoutArguments : Arguments() {
        public val user: Member by member {
            name = "member"
            description = "Member to apply a timeout to"
        }

        public val duration: DateTimePeriod? by optionalDuration {
            name = "duration"
            description = "How long to time out for, from now"

            validate {
                failIf(
                    "Timeouts must be for less than 28 days"
                ) { value != null && value!!.toTotalSeconds() >= MAX_TIMEOUT_SECS }
            }
        }
    }
}
