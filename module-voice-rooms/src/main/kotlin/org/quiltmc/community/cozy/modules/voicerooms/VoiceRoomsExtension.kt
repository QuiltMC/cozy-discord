/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.voicerooms

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.storage.StorageType
import com.kotlindiscord.kord.extensions.storage.StorageUnit
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.deltas.compare
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Role
import dev.kord.core.entity.VoiceState
import dev.kord.core.entity.channel.Channel
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.rest.builder.message.create.embed
import org.quiltmc.community.cozy.modules.voicerooms.data.GuildConfig
import org.quiltmc.community.cozy.modules.voicerooms.data.apply

public class VoiceRoomsExtension : Extension() {
	override val name: String = "quiltmc-voice-rooms"

	private val configUnit = StorageUnit<GuildConfig>(
		StorageType.Config,
		"voice-rooms",
		"config",
	)

	// 1. Configure the parent channel, logging channel and archiving category somehow
	// 2. Detect users joining it and create the channel with a set of defaults
	// 3. Move the user into their own channel and tell them how to manage it
	// 4. When the channel has been empty for a while, archive and delete it

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "voice-rooms"
			description = "Commands relating to voice rooms"

			allowInDms = false

			group("admin") {
				description = "Administrative commands"

				check { hasPermission(Permission.ManageGuild) }

				ephemeralSlashCommand {
					name = "get-settings"
					description = "Show the current server settings"

					action {
						val config = configUnit
							.withGuild(guild!!.id)
							.get()

						if (config == null) {
							respond { content = "No configuration found." }

							return@action
						}

						respond {
							embed { config.apply() }
						}
					}
				}

				ephemeralSlashCommand(::ServerSetupArgs) {
					name = "set-settings"
					description = "Change the current server settings"

					action {
						val config = GuildConfig(
							archiveCategory = arguments.archiveCategory.id,
							lobbyChannel = arguments.lobbyChannel.id,
							loggingChannel = arguments.loggingChannel.id,
							targetCategory = arguments.targetCategory.id,
						)

						configUnit
							.withGuild(guild!!.id)
							.save(config)

						respond {
							content = "Settings updated."

							embed { config.apply() }
						}
					}
				}

				ephemeralSlashCommand(::RoleArgs) {
					name = "add-moderator-role"
					description = "Add a moderator role"

					action {
						val unit = configUnit.withGuild(guild!!.id)
						val config = unit.get()

						if (config == null) {
							respond { content = "No configuration found." }

							return@action
						}

						if (arguments.role.id in config.moderatorRoles) {
							respond {
								content = "Role ${arguments.role.mention} is already marked as a moderator role."
							}
						}

						config.moderatorRoles.add(arguments.role.id)
						unit.save()
					}
				}

				ephemeralSlashCommand(::RoleArgs) {
					name = "remove-moderator-role"
					description = "Remove a moderator role"

					action {
						val unit = configUnit.withGuild(guild!!.id)
						val config = unit.get()

						if (config == null) {
							respond { content = "No configuration found." }

							return@action
						}

						if (arguments.role.id !in config.moderatorRoles) {
							respond {
								content = "Role ${arguments.role.mention} is not marked as a moderator role."
							}
						}

						config.moderatorRoles.remove(arguments.role.id)
						unit.save()
					}
				}

				// TODO: Command to kill a voice channel
			}

			group("configure") {
				description = "Configure your voice room"
			}

			group("defaults") {
				description = "Change your default settings for new voice rooms"
			}

			group("members") {
				description = "Configure who may or may not join your voice room"
			}
		}

		event<VoiceStateUpdateEvent> {
			action {
				val changes = event.old.compare(event.state)
				val channelState = changes[VoiceState::channelId]
			}
		}
	}

	public class ServerSetupArgs : Arguments() {
		public val archiveCategory: Channel by channel {
			name = "archive-category"
			description = "Category to use to temporary archive channels"

			requireChannelType(ChannelType.GuildCategory)
		}

		public val lobbyChannel: Channel by channel {
			name = "lobby-channel"
			description = "Voice channel people can join to create a new one"

			requireChannelType(ChannelType.GuildVoice)
			requireChannelType(ChannelType.GuildStageVoice)
		}

		public val loggingChannel: Channel by channel {
			name = "logging-channel"
			description = "Channel to use for logging"

			requireChannelType(ChannelType.GuildText)
			requireChannelType(ChannelType.PrivateThread)
			requireChannelType(ChannelType.PublicGuildThread)
			requireChannelType(ChannelType.PublicNewsThread)
		}

		public val targetCategory: Channel by channel {
			name = "target-category"
			description = "Category to create voice channels within"

			requireChannelType(ChannelType.GuildCategory)
		}
	}

	public class RoleArgs : Arguments() {
		public val role: Role by role {
			name = "role"
			description = "Role to add or remove"
		}
	}
}
