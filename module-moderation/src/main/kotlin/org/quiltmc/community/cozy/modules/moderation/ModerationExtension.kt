/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.isNotInThread
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.duration
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.channel.TextChannel
import kotlinx.datetime.DateTimePeriod
import org.quiltmc.community.cozy.modules.moderation.config.ModerationConfig

public val MAXIMUM_SLOWMODE_DURATION: DateTimePeriod = DateTimePeriod(hours = 6)

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

    override suspend fun setup() {
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

            validate() {
                failIf(
                    "Slowmode cannot be longer than ${MAXIMUM_SLOWMODE_DURATION.hours} hours"
                ) { value > MAXIMUM_SLOWMODE_DURATION }
            }
        }
    }
}
