/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.moderation

import com.kotlindiscord.kord.extensions.checks.isNotInThread
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.edit
import dev.kord.core.entity.channel.TextChannel
import org.quiltmc.community.GUILDS
import org.quiltmc.community.getCozyLogChannel
import org.quiltmc.community.hasBaseModeratorRole

class ModerationExtension : Extension() {
    override val name: String = "moderation"

    override suspend fun setup() {
        GUILDS.forEach { guildId ->
            ephemeralSlashCommand {
                name = "slowmode"
                description = "Manage slowmode of the current channel"

                guild(guildId)

                check { hasBaseModeratorRole() }
                check { isNotInThread() }

                ephemeralSubCommand {
                    name = "get"
                    description = "Get the slowmode of the channel"

                    action {
                        respond {
                            content = "Slowmode is currently " +
                                    "${channel.asChannel().data.rateLimitPerUser.value ?: 0} second(s)."
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
                            rateLimitPerUser = arguments.duration
                        }

                        guild?.asGuild()?.getCozyLogChannel()?.createEmbed {
                            title = "Slowmode changed"
                            description = "Set to ${arguments.duration} second(s)."

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
                            content = "Slowmode set to ${arguments.duration} second(s)."
                        }
                    }
                }
            }
        }
    }

    inner class SlowmodeEditArguments : Arguments() {
        val duration by int {
            name = "duration"
            description = "The new duration of the slowmode, in seconds"

            validate() {
                if (value < 0) {
                    error("Duration must be greater than 0")
                }
            }
        }
    }
}
