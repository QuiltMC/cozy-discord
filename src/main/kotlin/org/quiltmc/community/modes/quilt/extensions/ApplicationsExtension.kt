/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.*
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.events.extra.GuildJoinRequestDeleteEvent
import com.kotlindiscord.kord.extensions.events.extra.GuildJoinRequestUpdateEvent
import com.kotlindiscord.kord.extensions.events.extra.models.ApplicationStatus
import com.kotlindiscord.kord.extensions.events.extra.models.GuildJoinRequestResponse
import com.kotlindiscord.kord.extensions.extensions.*
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.getOfOrNull
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.Color
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.Member
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Instant
import org.koin.core.component.inject
import org.quiltmc.community.GUILDS
import org.quiltmc.community.database.collections.ServerApplicationCollection
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.entities.ServerApplication
import org.quiltmc.community.database.entities.ServerSettings
import org.quiltmc.community.hasBaseModeratorRole
import org.quiltmc.community.inQuiltGuild
import kotlin.time.Duration.Companion.seconds

private val COMPONENT_REGEX = "application/(\\d+)/(thread|verify)".toRegex()

class ApplicationsExtension : Extension() {
	override val name: String = "applications"

	private val serverSettings: ServerSettingsCollection by inject()
	private val applications: ServerApplicationCollection by inject()

	private val Instant.longAndRelative
		get() = "${toDiscord(TimestampType.LongDateTime)} (${toDiscord(TimestampType.RelativeTime)})"

	override suspend fun setup() {
		GUILDS.forEach {
			ephemeralSlashCommand {
				name = "applications"
				description = "Commands related to managing server applications"
				allowInDms = false

				guild(it)

				check { inQuiltGuild() }
				check { hasBaseModeratorRole(false) }

				ephemeralSubCommand(::ForceVerifyArguments) {
					name = "force-verify"
					description = "Make a user bypass Discord's verification process"

					action {
						val settings = serverSettings.get(guild!!.id)

						if (settings?.verificationRole == null || settings.moderationLogChannel == null) {
							respond {
								content =
									"This server doesn't have a configured verification role or moderation logging " +
											"channel."
							}

							return@action
						}

						val member = guild!!.getMemberOrNull(arguments.user.id)
						val modLog = guild!!.getChannelOf<TopGuildMessageChannel>(settings.moderationLogChannel!!)

						if (member == null) {
							respond {
								content = "User is not in this guild."
							}
						} else {
							member.addRole(settings.verificationRole!!)

							modLog.createEmbed {
								title = "User force verified"
								color = DISCORD_BLURPLE

								field {
									inline = true
									name = "Moderator"
									value = "${user.asUser().tag} (${user.mention})"
								}

								field {
									inline = true
									name = "User"
									value = "${member.tag} (${member.mention})"
								}
							}

							respond {
								content = "User ${member.mention} has been force verified."
							}
						}
					}
				}

				ephemeralSubCommand(::LookupArguments) {
					name = "lookup"
					description = "Look up a user's previous applications"

					action {
						val previousApplications = applications.findByUser(arguments.user.id).toList()

						editingPaginator {
							previousApplications.forEach { app ->
								page {
									title = "Previous Applications"
									color = app.status.toColor()

									description = buildString {
										appendLine("**Created:** ${app._id.timestamp.longAndRelative}")
										appendLine("**Status:** ${app.status?.name?.capitalizeWords() ?: "Withdrawn"}")
										appendLine("**User:** <@${app.userId}>")
										appendLine()

										if (app.actionedAt != null) {
											appendLine("**Actioned:** ${app.actionedAt!!.longAndRelative}")
										}

										if (app.rejectionReason != null) {
											appendLine()
											appendLine("**Rejection reason**")
											appendLine(
												app.rejectionReason!!
													.lines()
													.joinToString("\n") { "> $it" }
											)
										}

										appendLine("")

										if (app.messageLink != null) {
											appendLine()
											appendLine("[More details...](${app.messageLink})")
										}
									}
								}
							}
						}
					}
				}
			}

			ephemeralMessageCommand {
				name = "Fix Application Message"
				allowInDms = false

				guild(it)

				check { inQuiltGuild() }
				check { hasBaseModeratorRole(false) }

				action {
					val message = targetMessages.first()
					val application = applications.getByMessage(message.id)

					if (application == null) {
						respond {
							content = "Unable to find an application for this message."
						}

						return@action
					}

					val previousApplications = applications
						.findByUser(application.userId)
						.filter { it._id != application._id }
						.toList()

					message.edit {
						embed { message.embeds.first().apply(this) }
						embed { addPrevious(previousApplications) }

						actionRow {
							interactionButton(
								ButtonStyle.Secondary,
								"application/${application._id}/thread"
							) {
								emoji(ReactionEmoji.Unicode("✉️"))

								label = "Create Thread"
							}

							if (application.status == ApplicationStatus.Submitted) {
								interactionButton(
									ButtonStyle.Success,
									"application/${application._id}/verify"
								) {
									emoji(ReactionEmoji.Unicode("✅"))

									label = "Force Verify"
								}
							}
						}
					}

					respond {
						content = "Message updated."
					}
				}
			}
		}

		chatCommand(::ForceVerifyArguments) {
			name = "force-verify"
			description = "Make a user bypass Discord's verification process"

			check { inQuiltGuild() }
			check { hasBaseModeratorRole(false) }

			action {
				val settings = serverSettings.get(guild!!.id)

				if (settings?.verificationRole == null || settings.moderationLogChannel == null) {
					message.respond {
						content = "This server doesn't have a configured verification role or moderation logging " +
								"channel."
					}

					return@action
				}

				val member = guild!!.getMemberOrNull(arguments.user.id)
				val modLog = guild!!.getChannelOf<TopGuildMessageChannel>(settings.moderationLogChannel!!)

				if (member == null) {
					message.respond {
						content = "User is not in this guild."
					}
				} else {
					member.addRole(settings.verificationRole!!)

					modLog.createEmbed {
						title = "User force verified"
						color = DISCORD_BLURPLE

						field {
							inline = true
							name = "Moderator"
							value = "${user!!.asUser().tag} (${user!!.mention})"
						}

						field {
							inline = true
							name = "User"
							value = "${member.tag} (${member.mention})"
						}
					}

					message.respond {
						content = "User ${member.mention} has been force verified."
					}
				}
			}
		}

		event<ButtonInteractionCreateEvent> {
			check { inQuiltGuild() }
			check { hasBaseModeratorRole(false) }
			check { failIfNot(event.interaction.componentId.matches(COMPONENT_REGEX)) }

			check {
				val match = COMPONENT_REGEX.find(event.interaction.componentId)

				if (match == null) {
					fail()

					return@check
				}

				val guild = guildFor(event)!!
				val settings = serverSettings.get(guild.id)

				if (settings?.applicationLogChannel == null || settings.applicationThreadsChannel == null) {
					fail("Guild ${guild.id} does not have an application log or threads channel configured.")

					return@check
				}

				val applicationId = Snowflake(match.groupValues[1])
				val action = match.groupValues[2]

				val app = applications.get(applicationId)

				if (app == null) {
					fail("Unknown application: $applicationId")

					return@check
				}

				cache["action"] = action
				cache["application"] = app
				cache["serverSettings"] = settings
			}

			action {
				val action = cache.getOfOrNull<String>("action")!!
				val application = cache.getOfOrNull<ServerApplication>("application")!!
				val settings = cache.getOfOrNull<ServerSettings>("serverSettings")!!

				val guild = guildFor(event)!!
				val user = kord.getUser(application.userId)

				if (user == null) {
					event.interaction.respondEphemeral {
						content = "User that created this application can't be found or no longer exists."
					}

					return@action
				}

				when (action) {
					"thread" -> {
						val threadChannel = guild.getChannelOf<TextChannel>(settings.applicationLogChannel!!)

						if (application.threadId != null) {
							event.interaction.respondEphemeral {
								content = "A thread already exists for this application: <#${application.threadId}>"
							}
						} else {
							val thread = threadChannel.startPrivateThread(
								"App: ${user.tag}",
								ArchiveDuration.Week
							)

							val initialMessage = thread.createMessage("Better get the mods in...")

							initialMessage.edit { content = settings.moderatorRoles.joinToString { "<@&$it>" } }
							delay(2.seconds)

							initialMessage.edit {
								content = buildString {
									appendLine("**Application thread for ${user.tag}**")
									append("User ID below.")
								}
							}

							thread.createMessage("`${user.id}`")

							event.interaction.respondEphemeral {
								content = "Thread created: ${thread.mention}"
							}

							application.threadId = thread.id
							applications.save(application)
						}
					}

					"verify" -> {
						if (settings.verificationRole == null || settings.moderationLogChannel == null) {
							event.interaction.respondEphemeral {
								content =
									"This server doesn't have a configured verification role or moderation logging " +
											"channel."
							}

							return@action
						}

						val member = guild.getMemberOrNull(application.userId)
						val modLog = guild.getChannelOf<TopGuildMessageChannel>(settings.moderationLogChannel!!)

						if (member == null) {
							event.interaction.respondEphemeral {
								content = "User is not in this guild."
							}
						} else {
							member.addRole(settings.verificationRole!!)

							modLog.createEmbed {
								title = "User force verified"
								color = DISCORD_BLURPLE

								field {
									inline = true
									name = "Moderator"
									value = "${user.asUser().tag} (${user.mention})"
								}

								field {
									inline = true
									name = "User"
									value = "${member.tag} (${member.mention})"
								}
							}

							event.interaction.respondEphemeral {
								content = "User ${member.mention} has been force verified."
							}
						}
					}
				}
			}
		}

		event<GuildJoinRequestDeleteEvent> {
			check { inQuiltGuild() }

			check {
				val guild = guildFor(event)!!
				val settings = serverSettings.get(guild.id)

				if (settings?.applicationLogChannel == null || settings.applicationThreadsChannel == null) {
					fail("Guild ${guild.id} does not have an application log or threads channel configured.")

					return@check
				}

				val app = applications.get(event.requestId)

				if (app != null) {
					cache["application"] = app
				}

				cache["serverSettings"] = settings
			}

			action {
				delay(1.seconds)  // Just to make sure we get the update first

				val application = cache.getOfOrNull<ServerApplication>("application")
				val settings = cache.getOfOrNull<ServerSettings>("serverSettings")!!

				val logChannel = event.getGuild().getChannelOf<TextChannel>(settings.applicationLogChannel!!)

				var previousApplications = applications
					.findByUser(event.userId)
					.toList()

				// If null, an application was deleted that we're not keeping track of - happens when someone
				// leaves without applying as well, so we can't log it on Discord really

				if (application != null && application.status == ApplicationStatus.Submitted) {
					val message = logChannel.getMessage(application.messageId)

					previousApplications = previousApplications
						.filter { it._id != application._id }

					message.edit {
						embed {
							message.embeds.first().apply(this)

							title = "Application (Withdrawn)"
							color = DISCORD_WHITE
						}

						if (previousApplications.isNotEmpty()) {
							embed { addPrevious(previousApplications) }
						}
					}

					application.status = null
					applications.save(application)
				}
			}
		}

		event<GuildJoinRequestUpdateEvent> {
			check { inQuiltGuild() }

			check {
				val guild = guildFor(event)!!
				val settings = serverSettings.get(guild.id)

				if (settings?.applicationLogChannel == null || settings.applicationThreadsChannel == null) {
					fail("Guild ${guild.id} does not have an application log or threads channel configured.")

					return@check
				}

				val app = applications.get(event.requestId)

				if (app != null) {
					cache["application"] = app
				}

				cache["serverSettings"] = settings
			}

			action {
				var application = cache.getOfOrNull<ServerApplication>("application")
				val settings = cache.getOfOrNull<ServerSettings>("serverSettings")!!

				val logChannel = event.getGuild().getChannelOf<TextChannel>(settings.applicationLogChannel!!)
				var previousApplications = applications
					.findByUser(event.userId)
					.toList()

				if (application == null) {
					val message = logChannel.createMessage {
						embed { fromEvent(event) }

						if (previousApplications.isNotEmpty()) {
							embed { addPrevious(previousApplications) }
						}

						actionRow {
							interactionButton(
								ButtonStyle.Secondary,
								"application/${event.request.id}/thread"
							) {
								emoji(ReactionEmoji.Unicode("✉️"))

								label = "Create Thread"
							}

							if (event.status == ApplicationStatus.Submitted) {
								interactionButton(
									ButtonStyle.Success,
									"application/${event.request.id}/verify"
								) {
									emoji(ReactionEmoji.Unicode("✅"))

									label = "Force Verify"
								}
							}
						}
					}

					application = ServerApplication(
						_id = event.requestId,
						status = event.status,

						guildId = event.guildId,
						messageId = message.id,
						userId = event.userId,
						messageLink = message.getJumpUrl()
					)

					if (event.status == ApplicationStatus.Rejected) {
						application.actionedAt = event.request.actionedAt
						application.rejectionReason = event.request.rejectionReason
					}

					applications.save(application)
				} else {
					previousApplications = previousApplications
						.filter { it._id != application._id }

					val message = logChannel.getMessage(application.messageId)

					message.edit {
						embed { fromEvent(event) }

						if (previousApplications.isNotEmpty()) {
							embed { addPrevious(previousApplications) }
						}
					}

					application.actionedAt = event.request.actionedAt
					application.rejectionReason = event.request.rejectionReason
					application.status = event.request.status

					applications.save(application)
				}
			}
		}
	}

	private fun ApplicationStatus?.toColor(): Color = when (this) {
		ApplicationStatus.Approved -> DISCORD_GREEN
		ApplicationStatus.Rejected -> DISCORD_RED
		ApplicationStatus.Submitted -> DISCORD_YELLOW

		null -> DISCORD_WHITE
	}

	private fun EmbedBuilder.addPrevious(apps: List<ServerApplication>) {
		title = "Previous Applications: ${apps.size}"
		color = DISCORD_BLURPLE

		description = buildString {
			apps.sortedBy { it._id.timestamp }.forEachIndexed { i, app ->
				if (app.messageLink != null) {
					append("[${i + 1})](${app.messageLink}) ")
				} else {
					append("${i + 1}) ")
				}

				append(app.status?.name?.capitalizeWords() ?: "Withdrawn")

				if (app.actionedAt != null) {
					append(
						" at ${app.actionedAt!!.longAndRelative}"
					)
				}

				appendLine()

				if (app.rejectionReason != null) {
					appendLine(
						app.rejectionReason!!
							.lines()
							.joinToString("\n") { "> $it" }
					)
				}
			}
		}
	}

	private suspend fun EmbedBuilder.fromEvent(event: GuildJoinRequestUpdateEvent) {
		val user = event.getUser()

		color = event.request.status.toColor()
		title = "Application (${event.request.status.name.capitalizeWords()})"

		description = buildString {
			appendLine("**User:** ${user.tag}")
			appendLine("**Mention:** ${user.mention}")
			appendLine(
				"**Created:** ${user.id.timestamp.longAndRelative}"
			)

			if (event.request.actionedByUser != null) {
				val moderator = event.request.actionedByUser!!
				val time = event.request.actionedAt!!

				appendLine()
				appendLine(
					"**Actioned at:** ${time.longAndRelative}"
				)

				if (event.request.requestBypassed) {
					appendLine(
						"**Application bypassed via role assignment** "
					)
				} else {
					appendLine(
						"**Actioned by:** <@${moderator.id}> (`${moderator.username}#${moderator.discriminator}`)"
					)
				}

				if (event.request.rejectionReason != null) {
					appendLine()
					appendLine("**Rejection reason**")
					append(
						event.request.rejectionReason!!
							.lines()
							.joinToString("\n") { "> $it" }
					)
				}
			}
		}

		thumbnail {
			url = (user.avatar ?: user.defaultAvatar).url
		}

		footer {
			text = "User ID: ${event.userId}"
		}

		event.request.formResponses.forEach {
			field {
				name = it.label

				value = when (it) {
					is GuildJoinRequestResponse.TermsResponse ->
						if (it.response) {
							"✅ Accepted"
						} else {
							"❌ Not accepted"
						}

					is GuildJoinRequestResponse.MultipleChoiceResponse ->
						it.choices[it.response]

					is GuildJoinRequestResponse.ParagraphResponse ->
						it.response
				}
			}
		}
	}

	inner class LookupArguments : Arguments() {
		val user: User by user {
			name = "user"
			description = "Member to look up applications for"
		}
	}

	inner class ForceVerifyArguments : Arguments() {
		val user: Member by member {
			name = "member"
			description = "Member to verify"
		}
	}
}
