/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.*
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.events.extra.GuildJoinRequestDeleteEvent
import com.kotlindiscord.kord.extensions.events.extra.GuildJoinRequestUpdateEvent
import com.kotlindiscord.kord.extensions.events.extra.models.ApplicationStatus
import com.kotlindiscord.kord.extensions.events.extra.models.GuildJoinRequestResponse
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import com.kotlindiscord.kord.extensions.utils.getOfOrNull
import dev.kord.common.Color
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.delay
import org.koin.core.component.inject
import org.quiltmc.community.database.collections.ServerApplicationCollection
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.entities.ServerApplication
import org.quiltmc.community.database.entities.ServerSettings
import org.quiltmc.community.inQuiltGuild
import kotlin.time.Duration.Companion.seconds

private val COMPONENT_REGEX = "applicationThread/(\\d+)".toRegex()

class ApplicationsExtension : Extension() {
	override val name: String = "applications"

	private val serverSettings: ServerSettingsCollection by inject()
	private val applications: ServerApplicationCollection by inject()

	override suspend fun setup() {
		event<ButtonInteractionCreateEvent> {
			check { inQuiltGuild() }
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
				val app = applications.get(applicationId)

				if (app == null) {
					fail("Unknown application: $applicationId")

					return@check
				}

				cache["application"] = app
				cache["serverSettings"] = settings
			}

			action {
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
				val application = cache.getOfOrNull<ServerApplication>("application")
				val settings = cache.getOfOrNull<ServerSettings>("serverSettings")!!

				val logChannel = event.getGuild().getChannelOf<TextChannel>(settings.applicationLogChannel!!)

				if (application == null) {
					// Application was deleted that we're not keeping track of

					logChannel.createMessage {
						embed {
							title = "Unknown application deleted"
							color = DISCORD_FUCHSIA

							description = buildString {
								appendLine(
									"An application that this bot wasn't keeping track of has been deleted. " +
											"This may happen when an application is submitted while the bot is down, " +
											"or if the application was being handled by another bot."
								)
								appendLine()
								appendLine("**Application ID:** `${event.requestId}`")
								appendLine("**User:** <@${event.requestId}> (`${event.requestId}`)")
							}
						}
					}
				} else if (application.status == ApplicationStatus.Submitted) {
					val message = logChannel.getMessage(application.messageId)

					message.edit {
						embed {
							message.embeds[1].apply(this)

							title = "Application (Withdrawn)"
							color = DISCORD_WHITE
						}
					}
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

				if (application == null) {
					val message = logChannel.createMessage {
						embed { fromEvent(event) }

						actionRow {
							interactionButton(ButtonStyle.Secondary, "applicationThread/${event.request.id}") {
								emoji(ReactionEmoji.Unicode("✉️"))

								label = "Create Thread"
							}
						}
					}

					application = ServerApplication(
						_id = event.requestId,
						status = event.status,

						guildId = event.guildId,
						messageId = message.id,
						userId = event.userId,
					)

					applications.save(application)
				} else {
					val message = logChannel.getMessage(application.messageId)

					message.edit {
						embed { fromEvent(event) }
					}

					application.status = event.request.status
					applications.save(application)
				}
			}
		}
	}

	private fun ApplicationStatus.toColor(): Color = when (this) {
		ApplicationStatus.Approved -> DISCORD_GREEN
		ApplicationStatus.Rejected -> DISCORD_RED
		ApplicationStatus.Submitted -> DISCORD_YELLOW
	}

	private suspend fun EmbedBuilder.fromEvent(event: GuildJoinRequestUpdateEvent) {
		val user = event.getUser()

		color = event.request.status.toColor()
		title = "Application (${event.request.status.name.capitalizeWords()})"

		description = buildString {
			appendLine("**User:** ${user.tag}")
			appendLine("**Mention:** ${user.mention}")
			append(
				"**Created:** ${user.id.timestamp.toDiscord(TimestampType.LongDateTime)} " +
						"(${user.id.timestamp.toDiscord(TimestampType.RelativeTime)})"
			)
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
}
