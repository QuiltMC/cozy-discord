/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import kotlinx.coroutines.flow.toList
import org.koin.core.component.inject
import org.quiltmc.community.cozy.modules.welcome.config.WelcomeChannelConfig
import org.quiltmc.community.cozy.modules.welcome.data.WelcomeChannelData

/****/
public class WelcomeExtension : Extension() {
    override val name: String = "quiltmc-welcome"

    private val config: WelcomeChannelConfig by inject()
    private val data: WelcomeChannelData by inject()

    private val welcomeChannels: MutableMap<Snowflake, WelcomeChannel> = mutableMapOf()

    override suspend fun setup() {
        val initialMapping = data.getChannelURLs()

        event<GuildCreateEvent> {
            action {
                for (it in event.guild.channels.toList()) {
                    val channel = it.asChannelOfOrNull<GuildMessageChannel>()
                        ?: continue

                    val url = initialMapping[channel.id]
                        ?: continue

                    val welcomeChannel = WelcomeChannel(channel, url)

                    welcomeChannels[channel.id] = welcomeChannel

                    welcomeChannel.setup()
                }
            }
        }

        event<InteractionCreateEvent> {
            action {
                welcomeChannels[event.interaction.channelId]?.handleInteraction(event)
            }
        }

        ephemeralSlashCommand {
            name = "welcome-channels"
            description = "Manage welcome channels"

            config.getStaffCommandChecks().forEach(::check)

            ephemeralSubCommand(::ChannelArgs) {
                name = "delete"
                description = "Delete a welcome channel configuration"

                action {
                    val welcomeChannel = welcomeChannels[arguments.channel.id]

                    if (welcomeChannel != null) {
                        welcomeChannel.shutdown()
                        welcomeChannels.remove(arguments.channel.id)

                        val deletedUrl = data.removeChannel(arguments.channel.id)

                        respond {
                            content = "Configuration removed - old URL was `$deletedUrl`"
                        }
                    } else {
                        respond {
                            content = "No configuration for ${arguments.channel.mention} exists"
                        }
                    }
                }
            }

            ephemeralSubCommand(::ChannelArgs) {
                name = "get"
                description = "Get the url for a welcome channel, if it's configured"

                action {
                    val url = data.getUrlForChannel(arguments.channel.id)

                    respond {
                        content = if (url != null) {
                            "The configuration URL for ${arguments.channel.mention} is `$url`"
                        } else {
                            "No configuration for ${arguments.channel.mention} exists"
                        }
                    }
                }
            }

            ephemeralSubCommand(::ChannelRefreshArgs) {
                name = "refresh"
                description = "Manually repopulate the given welcome channel"

                action {
                    val welcomeChannel = welcomeChannels[arguments.channel.id]

                    respond {
                        content = if (welcomeChannel != null) {
                            "Manually refreshing ${arguments.channel.mention} now..."
                        } else {
                            "No configuration for ${arguments.channel.mention} exists"
                        }
                    }

                    if (arguments.clear) {
                        welcomeChannel?.clear()
                    }

                    welcomeChannel?.populate()
                }
            }

            ephemeralSubCommand(::ChannelCreateArgs) {
                name = "set"
                description = "Set the URL for a welcome channel, and populate it"

                action {
                    var welcomeChannel = welcomeChannels[arguments.channel.id]

                    if (welcomeChannel != null) {
                        welcomeChannels.remove(arguments.channel.id)
                        welcomeChannel.shutdown()
                    }

                    welcomeChannel = WelcomeChannel(arguments.channel.asChannelOf(), arguments.url)

                    data.setUrlForChannel(arguments.channel.id, arguments.url)
                    welcomeChannels[arguments.channel.id] = welcomeChannel

                    respond {
                        content = "Set the configuration URL for ${arguments.channel.mention} to `${arguments.url}`, " +
                                "clearing and refreshing..."
                    }

                    if (arguments.clear) {
                        welcomeChannel.clear()
                    }

                    welcomeChannel.setup()
                }
            }
        }
    }

    override suspend fun unload() {
        welcomeChannels.values.forEach { it.shutdown() }
        welcomeChannels.clear()
    }

    internal class ChannelCreateArgs : Arguments() {
        val channel by channel {
            name = "channel"
            description = "Channel representing a welcome channel"
        }

        val url by string {
            name = "url"
            description = "Public link to a YAML file used to configure a welcome channel"

            validate {
                failIf("URLs must contain a protocol (eg `https://`)") {
                    value.contains("://").not() ||
                            value.startsWith("://")
                }
            }
        }

        val clear by defaultingBoolean {
            name = "clear"
            description = "Whether to clear the channel before repopulating it"
            defaultValue = false
        }
    }

    internal class ChannelRefreshArgs : Arguments() {
        val channel by channel {
            name = "channel"
            description = "Channel representing a welcome channel"

            validate {
                failIf("Given channel must be a message channel on the current server") {
                    val guildChannel = value.asChannelOfOrNull<GuildMessageChannel>()

                    guildChannel == null || guildChannel.guildId != context.getGuild()?.id
                }
            }
        }

        val clear by defaultingBoolean {
            name = "clear"
            description = "Whether to clear the channel before repopulating it"
            defaultValue = false
        }
    }

    internal class ChannelArgs : Arguments() {
        val channel by channel {
            name = "channel"
            description = "Channel representing a welcome channel"

            validate {
                failIf("Given channel must be a message channel on the current server") {
                    val guildChannel = value.asChannelOfOrNull<GuildMessageChannel>()

                    guildChannel == null || guildChannel.guildId != context.getGuild()?.id
                }
            }
        }
    }
}
