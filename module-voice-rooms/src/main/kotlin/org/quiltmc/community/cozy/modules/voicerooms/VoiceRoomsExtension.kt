/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")

package org.quiltmc.community.cozy.modules.voicerooms

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.storage.StorageType
import com.kotlindiscord.kord.extensions.storage.StorageUnit
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.deltas.compare
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.VoiceState
import dev.kord.core.entity.channel.Channel
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.rest.builder.message.create.embed
import org.quiltmc.community.cozy.modules.voicerooms.data.DefaultSettings
import org.quiltmc.community.cozy.modules.voicerooms.data.GuildConfig
import org.quiltmc.community.cozy.modules.voicerooms.data.apply
import kotlin.math.min

public class VoiceRoomsExtension : Extension() {
	override val name: String = "quiltmc-voice-rooms"

	private val guildConfigUnit = StorageUnit<GuildConfig>(
		StorageType.Config,
		"voice-rooms",
		"guild-config",
	)

	private val defaultSettingsUnit = StorageUnit<DefaultSettings>(
		StorageType.Config,
		"voice-rooms",
		"default-settings",
	)

	// 1. Configure the parent channel, logging channel and archiving category somehow
	// 2. Detect users joining it and create the channel with a set of defaults
	// 3. Move the user into their own channel and tell them how to manage it
	// 4. When the channel has been empty for a while, archive and delete it

	override suspend fun setup() {
		// TODO: Make sure users are verified before allowing interactions

		ephemeralSlashCommand {
			name = "voice-rooms"
			description = "Commands relating to voice rooms"

			allowInDms = false

			group("admin") {
				description = "Administrative commands"

				check { hasPermission(Permission.ManageGuild) }

				ephemeralSubCommand {
					name = "get-settings"
					description = "Show the current server settings"

					action {
						val config = guildConfigUnit
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

				ephemeralSubCommand(::ServerSetupArgs) {
					name = "set-settings"
					description = "Change the current server settings"

					action {
						val config = GuildConfig(
							archiveCategory = arguments.archiveCategory.id,
							lobbyChannel = arguments.lobbyChannel.id,
							loggingChannel = arguments.loggingChannel.id,
							targetCategory = arguments.targetCategory.id,
						)

						guildConfigUnit
							.withGuild(guild!!.id)
							.save(config)

						respond {
							content = "Settings updated."

							embed { config.apply() }
						}
					}
				}

				ephemeralSubCommand(::RoleArgs) {
					name = "add-moderator-role"
					description = "Add a moderator role"

					action {
						val unit = guildConfigUnit.withGuild(guild!!.id)
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

				ephemeralSubCommand(::RoleArgs) {
					name = "remove-moderator-role"
					description = "Remove a moderator role"

					action {
						val unit = guildConfigUnit.withGuild(guild!!.id)
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

			group("defaults") {
				description = "Show or change the default settings for your voice rooms"

				ephemeralSubCommand {
					name = "get"
					description = "Show your default settings"

					action {
						val settings = defaultSettingsUnit
							.withUser(user)
							.get()
							?: DefaultSettings()

						editingPaginator {
							page {
								settings.apply()
							}

							if (settings.allowList.isNotEmpty()) {
								settings.allowList.chunked(20).forEach {
									page {
										title = "Default Configuration: Allow List"
										color = DISCORD_BLURPLE

										description = it.joinToString("\n") { "**»** <@$it> (`$it`)" }
									}
								}
							} else {
								page {
									title = "Default Configuration: Allow List"
									color = DISCORD_BLURPLE

									description = "No allow-listed users."
								}
							}

							if (settings.managers.isNotEmpty()) {
								settings.managers.chunked(20).forEach {
									page {
										title = "Default Configuration: Managers"
										color = DISCORD_BLURPLE

										description = it.joinToString("\n") { "**»** <@$it> (`$it`)" }
									}
								}
							} else {
								page {
									title = "Default Configuration: Managers"
									color = DISCORD_BLURPLE

									description = "No managers."
								}
							}

							if (settings.muted.isNotEmpty()) {
								settings.muted.chunked(20).forEach {
									page {
										title = "Default Configuration: Muted Users"
										color = DISCORD_BLURPLE

										description = it.joinToString("\n") { "**»** <@$it> (`$it`)" }
									}
								}
							} else {
								page {
									title = "Default Configuration: Muted Users"
									color = DISCORD_BLURPLE

									description = "No muted users."
								}
							}
						}
					}
				}

				ephemeralSubCommand(::NameArgs) {
					name = "name"
					description = "Set the default channel name, omit for more info"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						val member = member!!.asMember()

						if (arguments.name == null) {
							respond {
								content = "**Current name:** ${settings.name}\n\n" +
									"**Placeholders:**\n" +
									"**»** `{USERNAME}` -> Your username: `${member.username}`\n" +
									"**»** `{DISPLAY_NAME}` -> Your nickname/username: `${member.displayName}`\n" +
									"**»** `{TAG}` -> Your username and discriminator: `${member.tag}`\n" +
									"**»** `{DISCRIMINATOR}` -> Your discriminator: `${member.discriminator}`\n"
							}

							return@action
						}

						settings.name = arguments.name!!
						unit.save()

						val replaced = settings.name
							.replace("{USERNAME}", member.username)
							.replace("{DISPLAY_NAME}", member.displayName)
							.replace("{TAG}", member.tag)
							.replace("{DISCRIMINATOR}", member.discriminator)

						respond {
							content = "**Name set:** ${settings.name}" + if (replaced != settings.name) {
								"\n**»** $replaced"
							} else {
								""
							}
						}
					}
				}

				ephemeralSubCommand(::UserLimitArgs) {
					name = "user-limit"
					description = "Set the default user limit"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						settings.userLimit = min(arguments.limit, 0)
						unit.save()

						respond {
							content = if (settings.userLimit <= 0) {
								"User limit disabled."
							} else {
								"User limit set to ${settings.userLimit}."
							}
						}
					}
				}

				ephemeralSubCommand(::UserArgs) {
					name = "add-manager"
					description = "Add another user who can change the settings on your voice rooms"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						val id = arguments.member.id

						if (id in settings.managers) {
							respond {
								content = "${arguments.member.mention} is already a manager."
							}
						} else {
							settings.managers.add(id)
							unit.save()

							respond {
								content = "${arguments.member.mention} is now a manager."
							}
						}
					}
				}

				ephemeralSubCommand(::UserArgs) {
					name = "remove-manager"
					description = "Remove a manager you previously added"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						val id = arguments.member.id

						if (id !in settings.managers) {
							respond {
								content = "${arguments.member.mention} is not a manager."
							}
						} else {
							settings.managers.remove(id)
							unit.save()

							respond {
								content = "${arguments.member.mention} is no longer a manager."
							}
						}
					}
				}

				ephemeralSubCommand(::UserArgs) {
					name = "add-muted-user"
					description = "Add a user who may not speak in your voice rooms"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						val id = arguments.member.id

						if (id in settings.muted) {
							respond {
								content = "${arguments.member.mention} is already muted."
							}
						} else {
							settings.muted.add(id)
							unit.save()

							respond {
								content = "${arguments.member.mention} is now muted."
							}
						}
					}
				}

				ephemeralSubCommand(::UserArgs) {
					name = "remove-muted-user"
					description = "Remove a muted user"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						val id = arguments.member.id

						if (id !in settings.muted) {
							respond {
								content = "${arguments.member.mention} is not muted."
							}
						} else {
							settings.muted.remove(id)
							unit.save()

							respond {
								content = "${arguments.member.mention} is not longer muted."
							}
						}
					}
				}

				ephemeralSubCommand(::UserArgs) {
					name = "add-allowed-user"
					description = "Add a user to the allow-list"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						val id = arguments.member.id

						if (id in settings.allowList) {
							respond {
								content = "${arguments.member.mention} is already in the allow-list."
							}
						} else {
							settings.allowList.add(id)
							unit.save()

							respond {
								content = "${arguments.member.mention} has been added to the allow-list."
							}
						}
					}
				}

				ephemeralSubCommand(::UserArgs) {
					name = "remove-allowed-user"
					description = "Remove a user from the allow list"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						val id = arguments.member.id

						if (id !in settings.allowList) {
							respond {
								content = "${arguments.member.mention} is not in the allow-list."
							}
						} else {
							settings.allowList.add(id)
							unit.save()

							respond {
								content = "${arguments.member.mention} has been removed from the allow-list."
							}
						}
					}
				}

				ephemeralSubCommand(::ToggleArgs) {
					name = "toggle-activities"
					description = "Allow or disallow activities in your voice rooms"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						settings.allowActivities = arguments.value

						unit.save()

						respond {
							content = "Activities: ${arguments.value.enabled}"
						}
					}
				}

				ephemeralSubCommand(::ToggleArgs) {
					name = "toggle-allow-list"
					description = "Enable or disable an allow-list for channel participants"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						settings.allowListMode = arguments.value

						unit.save()

						respond {
							content = "Allow-list mode: ${arguments.value.enabled}"
						}
					}
				}

				ephemeralSubCommand(::ToggleArgs) {
					name = "toggle-soundboard"
					description = "Allow or disallow soundboards in your voice rooms"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						settings.allowSoundboard = arguments.value

						unit.save()

						respond {
							content = "Soundboards: ${arguments.value.enabled}"
						}
					}
				}

				ephemeralSubCommand(::ToggleArgs) {
					name = "toggle-text"
					description = "Allow or disallow text chat in your voice rooms"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						settings.allowText = arguments.value

						unit.save()

						respond {
							content = "Text chat: ${arguments.value.enabled}"
						}
					}
				}

				ephemeralSubCommand(::ToggleArgs) {
					name = "toggle-video"
					description = "Allow or disallow streaming and video in your voice rooms"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						settings.allowVideo = arguments.value

						unit.save()

						respond {
							content = "Video/streaming: ${arguments.value.enabled}"
						}
					}
				}

				ephemeralSubCommand(::ToggleArgs) {
					name = "toggle-voice-activity"
					description = "Allow or disallow voice activity in your voice rooms"

					action {
						val unit = defaultSettingsUnit
							.withUser(user)

						val settings = unit.get()
							?: DefaultSettings()

						settings.allowVoiceActivity = arguments.value

						unit.save()

						respond {
							content = "Voice activity: ${arguments.value.enabled}"
						}
					}
				}
			}

			group("configure") {
				description = "Configure your voice room"
			}

			group("members") {
				description = "Configure who may or may not join your voice room"
			}
		}

		event<VoiceStateUpdateEvent> {
			action {
				val changes = event.old.compare(event.state)
				val channelState = changes[VoiceState::channelId]

				println(channelState)
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

	public class NameArgs : Arguments() {
		public val name: String? by optionalString {
			name = "name"
			description = "Channel name, omit for more info"
		}
	}

	public class UserLimitArgs : Arguments() {
		public val limit: Int by int {
			name = "limit"
			description = "Channel user limit, 0 to disable"

			minValue = 0
			maxValue = 99
		}
	}

	public class RoleArgs : Arguments() {
		public val role: Role by role {
			name = "role"
			description = "Role to add or remove"
		}
	}

	public class UserArgs : Arguments() {
		public val member: Member by member {
			name = "user"
			description = "User to add or remove"

			requireSameGuild = true
		}
	}

	public class ToggleArgs : Arguments() {
		public val value: Boolean by boolean {
			name = "Enable"
			description = "Whether to enable or disable this toggle"
		}
	}
}
