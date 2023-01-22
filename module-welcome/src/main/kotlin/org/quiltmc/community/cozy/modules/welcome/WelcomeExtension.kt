/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome

import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
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
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
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

			allowInDms = false

			config.getStaffCommandChecks().forEach(::check)

			ephemeralSlashCommand(::ChannelArgs) {
				name = "blocks"
				description = "Get a list of the configured blocks"

				action {
					val welcomeChannel = welcomeChannels[arguments.channel.id]

					if (welcomeChannel == null) {
						respond {
							content = "No configuration for ${arguments.channel.mention} exists"
						}

						return@action
					}

					val blocks = welcomeChannel.getBlocks()

					respond {
						content = buildString {
							if (blocks.isEmpty()) {
								append("A configuration was found, but it doesn't contain any blocks.")
							} else {
								blocks.forEach {
									appendLine("**»** ${it.javaClass.simpleName}")
								}
							}
						}
					}
				}
			}

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

						welcomeChannel.log {
							embed {
								title = "Welcome channel removed"
								color = DISCORD_YELLOW

								description = "Welcome channel configuration removed."

								field {
									name = "Channel"
									value = "${welcomeChannel.channel.mention} (" +
											"`${welcomeChannel.channel.id}` / " +
											"`${welcomeChannel.channel.name}`" +
											")"
								}

								field {
									name = "Staff Member"
									value = "${user.mention} (" +
											"`${user.id}` / " +
											"`${user.asUser().tag}`" +
											")"
								}
							}
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

					if (welcomeChannel == null) {
						respond {
							content = "No configuration for ${arguments.channel.mention} exists"
						}

						return@action
					}

					respond {
						content = "Manually refreshing ${arguments.channel.mention} now..."
					}
					welcomeChannel.log {
						embed {
							title = "Welcome channel refreshed"
							color = DISCORD_YELLOW

							description = buildString {
								append("Manually ")

								if (arguments.clear) {
									append("**clearing** and ")
								}

								append("refreshing welcome channel...")
							}

							field {
								name = "Channel"
								value = "${welcomeChannel.channel.mention} (" +
										"`${welcomeChannel.channel.id}` / " +
										"`${welcomeChannel.channel.name}`" +
										")"
							}

							field {
								name = "Staff Member"
								value = "${user.mention} (" +
										"`${user.id}` / " +
										"`${user.asUser().tag}`" +
										")"
							}
						}
					}

					if (arguments.clear) {
						welcomeChannel.clear()
					}

					welcomeChannel.populate()
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
						content = buildString {
							append("Set the configuration URL for ${arguments.channel.mention} to `${arguments.url}`, ")

							if (arguments.clear) {
								append("clearing and ")
							}

							append("refreshing...")
						}
					}

					welcomeChannel.log {
						embed {
							title = "Welcome channel created/edited"
							color = DISCORD_YELLOW

							description = buildString {
								append("Welcome channel URL set: `${arguments.url}`")

								if (arguments.clear) {
									appendLine()
									appendLine("**Clearing channel...**")
								}
							}

							field {
								name = "Channel"
								value = "${welcomeChannel.channel.mention} (" +
										"`${welcomeChannel.channel.id}` / " +
										"`${welcomeChannel.channel.name}`" +
										")"
							}

							field {
								name = "Staff Member"
								value = "${user.mention} (" +
										"`${user.id}` / " +
										"`${user.asUser().tag}`" +
										")"
							}
						}
					}

					if (arguments.clear) {
						welcomeChannel.clear()
					}

					welcomeChannel.setup()
				}
			}
		}
	}

	public suspend fun log(channel: GuildMessageChannel, builder: UserMessageCreateBuilder.() -> Unit): Message? =
		config.getLoggingChannel(channel, channel.guild.asGuild())?.createMessage { builder() }

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
