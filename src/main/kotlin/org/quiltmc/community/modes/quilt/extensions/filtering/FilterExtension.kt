/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("CommentSpacing")  // I have no idea what you want from me

package org.quiltmc.community.modes.quilt.extensions.filtering

import com.github.curiousoddman.rgxgen.RgxGen
import com.github.curiousoddman.rgxgen.config.RgxGenOption
import com.github.curiousoddman.rgxgen.config.RgxGenProperties
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalEnumChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.deleteIgnoringNotFound
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.event.user.UserUpdateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging
import net.codebox.homoglyph.HomoglyphBuilder
import org.koin.core.component.inject
import org.quiltmc.community.*
import org.quiltmc.community.database.collections.FilterCollection
import org.quiltmc.community.database.collections.FilterEventCollection
import org.quiltmc.community.database.collections.GlobalSettingsCollection
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.entities.FilterEntry
import java.util.*
import kotlin.random.Random

const val FILTERS_PER_PAGE = 2

// Regex adapted by the regex used in the Python Discord bot, which is MIT-licensed.
// See LICENSE-python-discord for the license and copyright notice.

val INVITE_REGEX = (
		"(" +
				"discord(?:[\\.,]|dot)gg|" +  // discord.gg
				"discord(?:[\\.,]|dot)com(\\/|slash)invite|" +  // discord.com/invite/
				"discordapp(?:[\\.,]|dot)com(\\/|slash)invite|" +  //discordapp.com/invite/
				"discord(?:[\\.,]|dot)me|" +  // discord.me
				"discord(?:[\\.,]|dot)li|" +  // discord.li
				"discord(?:[\\.,]|dot)io|" +  // discord.io
				"(?:(?<!\\w)(?:[\\.,]|dot))gg" +  // discord.gg
				")(?:[\\/]|slash)" +  // ... / or "slash"
				"(?<invite>[a-zA-Z0-9\\-]+)" +  // invite code
				""
		).toRegex(RegexOption.IGNORE_CASE)

val OTP_REGEX = "(cc|vv|en)[cbdefghijklnrtuv]{42}".toRegex()

val OTP_BAD = setOf("BAD_OTP", "REPLAYED_OTP")

const val OTP_NONCE_BYTES = 15

const val OTP_URL = "https://api.yubico.com/wsapi/2.0/verify"

class FilterExtension : Extension() {
	override val name: String = "filter"
	private val logger = KotlinLogging.logger { }

	private val rgxProperties = RgxGenProperties()
	private val inviteCache: MutableMap<String, Snowflake> = mutableMapOf()

	private val globalSettings: GlobalSettingsCollection by inject()
	private val serverSettings: ServerSettingsCollection by inject()

	private val homoglyphs = HomoglyphBuilder.build()

	private val client = HttpClient()

	init {
		RgxGenOption.INFINITE_PATTERN_REPETITION.setInProperties(rgxProperties, 2)
		RgxGen.setDefaultProperties(rgxProperties)
	}

	val filters: FilterCollection by inject()
	val filterCache: MutableMap<UUID, FilterEntry> = mutableMapOf()
	val filterEvents: FilterEventCollection by inject()

	override suspend fun setup() {
		reloadFilters()

		event<MessageCreateEvent> {
			check { event.message.author != null }
			check { isNotBot() }
			check { inQuiltGuild() }
			check { notHasBaseModeratorRole() }

			action {
				handleMessage(event.message)
			}
		}

		event<MessageUpdateEvent> {
			check { event.message.asMessageOrNull()?.author != null }
			check { isNotBot() }
			check { inQuiltGuild() }
			check { notHasBaseModeratorRole() }

			action {
				handleMessage(event.message.asMessage())
			}
		}

		event<MemberJoinEvent> {
			check { isNotBot() }
			check { inQuiltGuild() }
			check { notHasBaseModeratorRole() }

			action {
				handleMember(event.member)
			}
		}

		event<MemberUpdateEvent> {
			check { isNotBot() }
			check { inQuiltGuild() }
			check { notHasBaseModeratorRole() }

			action {
				handleMember(event.member)
			}
		}

		event<UserUpdateEvent> {
			check { isNotBot() }
			check { inQuiltGuild() }
			check { notHasBaseModeratorRole() }

			action {
				val guilds = serverSettings.getByQuiltServers().toList().mapNotNull {
					kord.getGuild(it._id)
				}

				val members = guilds.mapNotNull { it.getMemberOrNull(event.user.id) }.toList()

				members.forEach { handleMember(it) }
			}
		}

		GUILDS.forEach { guildId ->
			ephemeralSlashCommand {
				name = "filters"
				description = "Filter management commands"

				allowInDms = false

				check { hasBaseModeratorRole(false) }

				guild(guildId)

				ephemeralSubCommand(::FilterEditMatchArgs) {
					name = "edit_match"
					description = "Update the match for a given filter"

					action {
						val filter = filters.get(UUID.fromString(arguments.uuid))

						if (filter == null) {
							respond {
								content = "No such filter: `${arguments.uuid}`"
							}

							return@action
						}

						filter.match = arguments.match
						filters.set(filter)
						filterCache[filter._id] = filter

						this@FilterExtension.kord.getGuild(COMMUNITY_GUILD)
							?.getCozyLogChannel()
							?.createEmbed {
								color = DISCORD_GREEN
								title = "Filter match updated"

								formatFilter(filter)

								field {
									name = "Moderator"
									value = "${user.mention} (`${user.id.value}` / `${user.asUser().tag}`)"
								}
							}

						respond {
							embed {
								color = DISCORD_GREEN
								title = "Filter match updated"

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand(::FilterEditMatchTypeArgs) {
					name = "edit_match_type"
					description = "Update the match type for a given filter"

					action {
						val filter = filters.get(UUID.fromString(arguments.uuid))

						if (filter == null) {
							respond {
								content = "No such filter: `${arguments.uuid}`"
							}

							return@action
						}

						filter.matchType = arguments.matchType
						filters.set(filter)
						filterCache[filter._id] = filter

						this@FilterExtension.kord.getGuild(COMMUNITY_GUILD)
							?.getCozyLogChannel()
							?.createEmbed {
								color = DISCORD_GREEN
								title = "Filter match type updated"

								formatFilter(filter)

								field {
									name = "Moderator"
									value = "${user.mention} (`${user.id.value}` / `${user.asUser().tag}`)"
								}
							}

						respond {
							embed {
								color = DISCORD_GREEN
								title = "Filter match type updated"

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand(::FilterEditActionArgs) {
					name = "edit_action"
					description = "Update the action for a given filter"

					action {
						val filter = filters.get(UUID.fromString(arguments.uuid))

						if (filter == null) {
							respond {
								content = "No such filter: `${arguments.uuid}`"
							}

							return@action
						}

						if (filter.matchTarget == MatchTarget.USER && arguments.action?.validForUsers == false) {
							respond {
								content = "The given action (**${arguments.action?.readableName}**) is not valid " +
										"for filters that target user profiles."
							}

							return@action
						}

						if (arguments.action == FilterAction.RESPOND && filter.note == null) {
							respond {
								content = "The given action (**${arguments.action?.readableName}**) is not valid " +
										"for filters that do not have a note set, as the note will be sent in " +
										"response to users that trigger the filter."
							}

							return@action
						}

						filter.action = arguments.action
						filters.set(filter)
						filterCache[filter._id] = filter

						this@FilterExtension.kord.getGuild(COMMUNITY_GUILD)
							?.getCozyLogChannel()
							?.createEmbed {
								color = DISCORD_GREEN
								title = "Filter action updated"

								formatFilter(filter)

								field {
									name = "Moderator"
									value = "${user.mention} (`${user.id.value}` / `${user.asUser().tag}`)"
								}
							}

						respond {
							embed {
								color = DISCORD_GREEN
								title = "Filter action updated"

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand(::FilterEditTargetArgs) {
					name = "edit_target"
					description = "Update the content target type for a given filter"

					action {
						val filter = filters.get(UUID.fromString(arguments.uuid))

						if (filter == null) {
							respond {
								content = "No such filter: `${arguments.uuid}`"
							}

							return@action
						}

						if (arguments.target == MatchTarget.USER && filter.action?.validForUsers == false) {
							respond {
								content = "The filter's action (**${filter.action?.readableName}**) is not valid " +
										"for filters that target user profiles."
							}

							return@action
						}

						filter.matchTarget = arguments.target
						filters.set(filter)
						filterCache[filter._id] = filter

						this@FilterExtension.kord.getGuild(COMMUNITY_GUILD)
							?.getCozyLogChannel()
							?.createEmbed {
								color = DISCORD_GREEN
								title = "Filter target updated"

								formatFilter(filter)

								field {
									name = "Moderator"
									value = "${user.mention} (`${user.id.value}` / `${user.asUser().tag}`)"
								}
							}

						respond {
							embed {
								color = DISCORD_GREEN
								title = "Filter target updated"

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand(::FilterEditPingArgs) {
					name = "edit_ping"
					description = "Update the ping setting for a given filter"

					action {
						val filter = filters.get(UUID.fromString(arguments.uuid))

						if (filter == null) {
							respond {
								content = "No such filter: `${arguments.uuid}`"
							}

							return@action
						}

						filter.pingStaff = arguments.ping
						filters.set(filter)
						filterCache[filter._id] = filter

						this@FilterExtension.kord.getGuild(COMMUNITY_GUILD)
							?.getCozyLogChannel()
							?.createEmbed {
								color = DISCORD_GREEN
								title = "Filter ping setting updated"

								formatFilter(filter)

								field {
									name = "Moderator"
									value = "${user.mention} (`${user.id.value}` / `${user.asUser().tag}`)"
								}
							}

						respond {
							embed {
								color = DISCORD_GREEN
								title = "Filter ping setting updated"

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand(::FilterEditNoteArgs) {
					name = "edit_note"
					description = "Update the note for a given filter"

					action {
						val filter = filters.get(UUID.fromString(arguments.uuid))

						if (filter == null) {
							respond {
								content = "No such filter: `${arguments.uuid}`"
							}

							return@action
						}

						if (filter.action == FilterAction.RESPOND && arguments.note == null) {
							respond {
								content = "Responding filters must include a note, which will be sent to the user in" +
										"response."
							}

							return@action
						}

						filter.note = arguments.note
						filters.set(filter)
						filterCache[filter._id] = filter

						this@FilterExtension.kord.getGuild(COMMUNITY_GUILD)
							?.getCozyLogChannel()
							?.createEmbed {
								color = DISCORD_GREEN
								title = "Filter note updated"

								formatFilter(filter)

								field {
									name = "Moderator"
									value = "${user.mention} (`${user.id.value}` / `${user.asUser().tag}`)"
								}
							}

						respond {
							embed {
								color = DISCORD_GREEN
								title = "Filter note updated"

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand(::FilterCreateArgs) {
					name = "create"
					description = "Create a new filter"

					action {
						if (arguments.target == MatchTarget.USER && arguments.action?.validForUsers == false) {
							respond {
								content = "The given action (**${arguments.action?.readableName}**) is not valid " +
										"for filters that target user profiles."
							}

							return@action
						}

						if (arguments.action == FilterAction.RESPOND && arguments.note == null) {
							respond {
								content = "Responding filters must include a note, which will be sent to the user in" +
										"response."
							}

							return@action
						}

						val filter = FilterEntry(
							_id = UUID.randomUUID(),

							action = arguments.action,
							pingStaff = arguments.ping,

							match = arguments.match,
							matchType = arguments.matchType,

							note = arguments.note,
							matchTarget = arguments.target
						)

						filters.set(filter)
						filterCache[filter._id] = filter

						this@FilterExtension.kord.getGuild(COMMUNITY_GUILD)
							?.getCozyLogChannel()
							?.createEmbed {
								color = DISCORD_GREEN
								title = "Filter created"

								formatFilter(filter)

								field {
									name = "Moderator"
									value = "${user.mention} (`${user.id.value}` / `${user.asUser().tag}`)"
								}
							}

						respond {
							embed {
								color = DISCORD_GREEN
								title = "Filter created"

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand(::FilterIDArgs) {
					name = "delete"
					description = "Delete an existing filter"

					action {
						val filter = filters.get(UUID.fromString(arguments.uuid))

						if (filter == null) {
							respond {
								content = "No such filter: `${arguments.uuid}`"
							}

							return@action
						}

						filters.remove(filter)
						filterCache.remove(filter._id)

						this@FilterExtension.kord.getGuild(COMMUNITY_GUILD)
							?.getCozyLogChannel()
							?.createEmbed {
								title = "Filter deleted"
								color = DISCORD_RED

								formatFilter(filter)

								field {
									name = "Moderator"
									value = "${user.mention} (`${user.id.value}` / `${user.asUser().tag}`)"
								}
							}

						respond {
							embed {
								title = "Filter deleted"
								color = DISCORD_RED

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand(::FilterIDArgs) {
					name = "get"
					description = "Get information about a specific filter"

					action {
						val filter = filters.get(UUID.fromString(arguments.uuid))

						if (filter == null) {
							respond {
								content = "No such filter: `${arguments.uuid}`"
							}

							return@action
						}

						respond {
							embed {
								color = DISCORD_BLURPLE
								title = "Filter info"

								formatFilter(filter)
							}
						}
					}
				}

				ephemeralSubCommand {
					name = "list"
					description = "List all filters"

					action {
						val filters = filterCache.values

						if (filters.isEmpty()) {
							respond {
								content = "No filters have been created."
							}

							return@action
						}

						editingPaginator(locale = getLocale()) {
							filters
								.sortedByDescending { it.action?.severity ?: -1 }
								.chunked(FILTERS_PER_PAGE)
								.forEach { filters ->
									page {
										color = DISCORD_BLURPLE
										title = "Filters"

										filters.forEach { formatFilter(it) }
									}
								}
						}.send()
					}
				}

				ephemeralSubCommand {
					name = "reload"
					description = "Reload filters from the database"

					action {
						reloadFilters()

						respond {
							content = "Reloaded ${filterCache.size} filters."
						}
					}
				}
			}
		}
	}

	@Suppress("TooGenericExceptionCaught")
	suspend fun handleMessage(message: Message) {
		val matched = filterCache.values
			.filter { it.matchTarget == MatchTarget.MESSAGE }
			.sortedByDescending { it.action?.severity ?: -1 }

		for (filter in matched) {
			try {
				if (filter.matches(message.content)) {
					filter.action(message)

					return
				}
			} catch (t: Throwable) {
				logger.error(t) { "Failed to check filter ${filter._id}" }
			}
		}
	}

	@Suppress("TooGenericExceptionCaught")
	suspend fun handleMember(member: Member) {
		val matched = filterCache.values
			.filter { it.matchTarget == MatchTarget.USER }
			.sortedByDescending { it.action?.severity ?: -1 }

		for (filter in matched) {
			try {
				if (
					filter.matches(member.username) ||
					(member.nickname != null && filter.matches(member.nickname!!)) ||

					member.getPresenceOrNull()?.activities?.any {
						filter.matches(it.name) || (it.details != null && filter.matches(it.details!!))
					} == true
				) {
					filter.action(member)

					return
				}
			} catch (t: Throwable) {
				logger.error(t) { "Failed to check filter ${filter._id}" }
			}
		}
	}

	// TODO: Provide profile content
	suspend fun FilterEntry.action(member: Member) {
		val guild = member.getGuild()

		if (action?.validForUsers == false) {
			error(
				"The filter's action is not valid for user profiles.\n" +
						"Filter: $_id,\n" +
						"Action: ${action?.readableName}"
			)
		}

		when (action) {
			FilterAction.KICK -> {
				member.dm {
					content = "You have been kicked from **${guild.name}** for something in your user " +
							"profile."
				}

				member.kick("Kicked by filter: $_id")
			}

			FilterAction.BAN -> {
				member.dm {
					content = "You have been kicked from **${guild.name}** for something in your user " +
							"profile.\n\n" +

							"If you'd like to appeal your ban: https://discord.gg/" +
							"${globalSettings.get()?.appealsInvite}"
				}

				member.ban {
					reason = "Banned by filter: $_id"
				}
			}

			else -> {}  // Nothing
		}

		filterEvents.add(
			this, guild, member, null, null
		)

		guild.getFilterLogChannel()?.createMessage {
			if (pingStaff) {
				val modRole = when (guild.id) {
					COMMUNITY_GUILD -> guild.getRole(COMMUNITY_MODERATOR_ROLE)
					TOOLCHAIN_GUILD -> guild.getRole(TOOLCHAIN_MODERATOR_ROLE)

					else -> null
				}

				content = modRole?.mention
					?: "**Warning:** This filter shouldn't have triggered on this server! This is a bug!"
			}

			embed {
				color = DISCORD_YELLOW
				title = "Filter triggered!"

				field {
					inline = true
					name = "User"
					value = "${member.mention} (`${member.id}` / `${member.tag}`)"
				}

				field {
					inline = true
					name = "Username"
					value = member.username
				}

				if (member.nickname != null) {
					field {
						inline = true
						name = "Nickname"
						value = member.nickname!!
					}
				}

				val presence = member.getPresenceOrNull()

				if (presence != null && presence.activities.isNotEmpty()) {
					field {
						name = "Activities"
						value = presence.activities.joinToString("\n") {
							"**${it.name}**" + if (it.details != null) {
								": ${it.details}"
							} else {
								""
							}
						}
					}
				}

				field {
					name = "Filter ID"
					value = "`$_id`"
				}

				field {
					inline = true
					name = "Action"
					value = action?.readableName ?: "Log only"
				}

				field {
					inline = true
					name = "Match Type"
					value = matchType.readableName
				}

				field {
					inline = true
					name = "Target Type"
					value = matchTarget.readableName
				}

				if (note != null) {
					field {
						name = "Filter Note"
						value = note!!
					}
				}

				field {
					name = "Match String"

					value = "```\n" +

							if (matchType == MatchType.INVITE) {
								"Guild ID: $match\n"
							} else {
								"$match\n"
							} +

							"```"
				}
			}
		}
	}

	suspend fun FilterEntry.action(message: Message) {
		val guild = message.getGuild()

		when (action) {
			FilterAction.DELETE -> {
				message.author!!.dm {
					content = "The message you just sent on **${guild.name}** has been automatically removed."

					embed {
						description = message.content

						field {
							name = "Channel"
							value = "${message.channel.mention} (`${message.channel.id.value}`)"
						}

						field {
							name = "Message ID"
							value = "`${message.id}`"
						}
					}
				}

				message.deleteIgnoringNotFound()
			}

			FilterAction.KICK -> {
				message.deleteIgnoringNotFound()

				message.author!!.dm {
					content = "You have been kicked from **${guild.name}** for sending the below message."

					embed {
						description = message.content

						field {
							name = "Channel"
							value = "${message.channel.mention} (`${message.channel.id.value}`)"
						}

						field {
							name = "Message ID"
							value = "`${message.id}`"
						}
					}
				}

				message.author!!.asMember(message.getGuild().id).kick("Kicked by filter: $_id")
			}

			FilterAction.BAN -> {
				message.deleteIgnoringNotFound()

				message.author!!.dm {
					content = "You have been banned from **${guild.name}** for sending the below message.\n\n" +

							"If you'd like to appeal your ban: https://discord.gg/" +
							"${globalSettings.get()?.appealsInvite}"

					embed {
						description = message.content

						field {
							name = "Channel"
							value = "${message.channel.mention} (`${message.channel.id.value}`)"
						}

						field {
							name = "Message ID"
							value = "`${message.id}`"
						}
					}
				}

				message.author!!.asMember(message.getGuild().id).ban {
					reason = "Banned by filter: $_id"
				}
			}

			FilterAction.RESPOND -> message.respond {
				content = "**An automated message from the moderators:** $note"
			}

			null -> {}  // Nothing
		}

		filterEvents.add(
			this, message.getGuild(), message.author!!, message.channel, message
		)

		guild.getFilterLogChannel()?.createMessage {
			if (pingStaff) {
				val modRole = when (guild.id) {
					COMMUNITY_GUILD -> guild.getRole(COMMUNITY_MODERATOR_ROLE)
					TOOLCHAIN_GUILD -> guild.getRole(TOOLCHAIN_MODERATOR_ROLE)

					else -> null
				}

				content = modRole?.mention
					?: "**Warning:** This filter shouldn't have triggered on this server! This is a bug!"
			}

			embed {
				color = DISCORD_YELLOW
				title = "Filter triggered!"
				description = message.content

				field {
					inline = true
					name = "Author"
					value = "${message.author!!.mention} (`${message.author!!.id.value}` / `${message.author!!.tag}`)"
				}

				field {
					inline = true
					name = "Channel"
					value = message.channel.mention
				}

				field {
					inline = true
					name = "Message"
					value = "[`${message.id.value}`](${message.getJumpUrl()})"
				}

				field {
					name = "Filter ID"
					value = "`$_id`"
				}

				field {
					inline = true
					name = "Action"
					value = action?.readableName ?: "Log only"
				}

				field {
					inline = true
					name = "Match Type"
					value = matchType.readableName
				}

				field {
					inline = true
					name = "Target Type"
					value = matchTarget.readableName
				}

				if (note != null) {
					field {
						name = "Filter Note"
						value = note!!
					}
				}

				field {
					name = "Match String"

					value = "```\n" +

							if (matchType == MatchType.INVITE) {
								"Suild ID: $match\n"
							} else {
								"$match\n"
							} +

							"```"
				}
			}
		}
	}

	suspend fun FilterEntry.matches(content: String): Boolean = when (matchType) {
		MatchType.CONTAINS -> homoglyphs.search(match.lowercase()).size > 0
		MatchType.EXACT -> content.equals(match, ignoreCase = true)
		MatchType.REGEX -> match.toRegex(RegexOption.IGNORE_CASE).matches(content)
		MatchType.REGEX_CONTAINS -> content.contains(match.toRegex(RegexOption.IGNORE_CASE))
		MatchType.INVITE -> content.containsInviteFor(Snowflake(match))
		MatchType.YUBICO_OTP -> content.validateYubicoOtp(match)
	}

	suspend fun String.containsInviteFor(guild: Snowflake): Boolean {
		val inviteMatches = INVITE_REGEX.findAll(this)

		for (match in inviteMatches) {
			val code = match.groups["invite"]!!.value

			if (code in inviteCache) {
				if (guild == inviteCache[code]) {
					return true
				} else {
					continue
				}
			}

			val invite = kord.getInviteOrNull(code, false)
			val inviteGuild = invite?.partialGuild?.id ?: continue

			inviteCache[code] = inviteGuild

			if (inviteGuild == guild) {
				return true
			}
		}

		return false
	}

	suspend fun String.validateYubicoOtp(clientId: String): Boolean {
		if (!OTP_REGEX.matches(this)) {
			return false
		}

		val otp = this

		val prefix = otp.substring(0, 2)

		if (prefix == "en") { // not yubicloud
			return true
		}

		val nonceBytes = Random.nextBytes(OTP_NONCE_BYTES)
		val nonce = HexFormat.of().formatHex(nonceBytes)

		val responseText: String = client.get(OTP_URL) {
			url {
				parameters.append("id", clientId)
				parameters.append("nonce", nonce)
				parameters.append("otp", otp)
			}
		}.body()

		val response = responseText
			.trim()
			.lines()
			.map { it.split("=", limit = 2) }
			.map { it.first() to it.last() }
			.toMap()

		if (response["nonce"] != nonce) {
			logger.error { "Invalid nonce from Yubicloud: ${response["nonce"]}" }
			return false
		}

		if (response["status"] == "OK") {
			logger.debug { "Validated OTP $otp" }

			return true
		}

		if (response["status"] in OTP_BAD) {
			logger.debug { "OTP $otp invalid: ${response["status"]}" }

			return false
		}

		logger.warn { "Unexpected status for OTP $otp: ${response["status"]}" }

		return false
	}

	suspend fun reloadFilters() {
		filterCache.clear()

		filters.getAll().forEach {
			filterCache[it._id] = it
		}
	}

	@Suppress("TooGenericExceptionCaught")
	fun EmbedBuilder.formatFilter(filter: FilterEntry) {
		if (description == null) {
			description = ""
		}

		description += "__**${filter._id}**__\n\n" +
				"**Action:** ${filter.action?.readableName ?: "Log only"}\n" +
				"**Match type:** ${filter.matchType.readableName}\n" +
				"**Target type:** ${filter.matchTarget.readableName}\n" +
				"**Ping staff:** ${if (filter.pingStaff) "Yes" else "No"}\n\n" +

				if (filter.note != null) {
					"**__Staff Note__**\n${filter.note}\n\n"
				} else {
					""
				} +

				"__**Match**__\n\n" +

				"```\n" +
				"${filter.match}\n" +
				"```\n"

		if (filter.matchType == MatchType.REGEX || filter.matchType == MatchType.REGEX_CONTAINS) {
			try {
				val generator = RgxGen(filter.match)
				val examples = mutableSetOf<String>()

				repeat(FILTERS_PER_PAGE * 2) {
					examples.add(generator.generate())
				}

				if (examples.isNotEmpty()) {
					description += "__**Examples**__\n\n" +

							"```\n" +
							examples.joinToString("\n") +
							"```\n"
				}
			} catch (t: Throwable) {
				logger.error(t) { "Failed to generate examples for regular expression: ${filter.match}" }

				description += "__**Examples**__\n\n" +

						"**Failed to generate examples: `${t.message}`\n"
			}

			description += "\n"
		}
	}

	@Suppress("TooGenericExceptionCaught")
	inner class FilterIDArgs : Arguments() {
		val uuid by string {
			name = "uuid"
			description = "Filter ID"

			validate {
				try {
					UUID.fromString(value)
				} catch (t: Throwable) {
					fail("Please provide a valid UUID")
				}
			}
		}
	}

	inner class FilterCreateArgs : Arguments() {
		val match by string {
			name = "match"
			description = "Text to match on"
		}

		val matchType by enumChoice<MatchType> {
			name = "match-type"
			description = "Type of match"

			typeName = "match type`"
		}

		val target by enumChoice<MatchTarget> {
			name = "target"
			description = "Content type to target"

			typeName = "target"
		}

		val action by optionalEnumChoice<FilterAction> {
			name = "action"
			description = "Action to take"

			typeName = "action"
		}

		val ping by defaultingBoolean {
			name = "ping"
			description = "Whether to ping the moderators"

			defaultValue = false
		}

		val note by optionalString {
			name = "note"
			description = "Note explaining what this filter is for"
		}
	}

	@Suppress("TooGenericExceptionCaught")
	inner class FilterEditMatchArgs : Arguments() {
		val uuid by string {
			name = "uuid"
			description = "Filter ID"

			validate {
				try {
					UUID.fromString(value)
				} catch (t: Throwable) {
					fail("Please provide a valid UUID")
				}
			}
		}

		val match by string {
			name = "match"
			description = "Text to match on"
		}
	}

	@Suppress("TooGenericExceptionCaught")
	inner class FilterEditMatchTypeArgs : Arguments() {
		val uuid by string {
			name = "uuid"
			description = "Filter ID"

			validate {
				try {
					UUID.fromString(value)
				} catch (t: Throwable) {
					fail("Please provide a valid UUID")
				}
			}
		}

		val matchType by enumChoice<MatchType> {
			name = "match-type"
			description = "Type of match"

			typeName = "match type`"
		}
	}

	@Suppress("TooGenericExceptionCaught")
	inner class FilterEditActionArgs : Arguments() {
		val uuid by string {
			name = "uuid"
			description = "Filter ID"

			validate {
				try {
					UUID.fromString(value)
				} catch (t: Throwable) {
					fail("Please provide a valid UUID")
				}
			}
		}

		val action by optionalEnumChoice<FilterAction> {
			name = "action"
			description = "Action to take"

			typeName = "action"
		}
	}

	@Suppress("TooGenericExceptionCaught")
	inner class FilterEditTargetArgs : Arguments() {
		val uuid by string {
			name = "uuid"
			description = "Filter ID"

			validate {
				try {
					UUID.fromString(value)
				} catch (t: Throwable) {
					fail("Please provide a valid UUID")
				}
			}
		}

		val target by enumChoice<MatchTarget> {
			name = "target"
			description = "Content type to target"

			typeName = "target"
		}
	}

	@Suppress("TooGenericExceptionCaught")
	inner class FilterEditPingArgs : Arguments() {
		val uuid by string {
			name = "uuid"
			description = "Filter ID"

			validate {
				try {
					UUID.fromString(value)
				} catch (t: Throwable) {
					fail("Please provide a valid UUID")
				}
			}
		}

		val ping by defaultingBoolean {
			name = "ping"
			description = "Whether to ping the moderators"

			defaultValue = false
		}
	}

	@Suppress("TooGenericExceptionCaught")
	inner class FilterEditNoteArgs : Arguments() {
		val uuid by string {
			name = "uuid"
			description = "Filter ID"

			validate {
				try {
					UUID.fromString(value)
				} catch (t: Throwable) {
					fail("Please provide a valid UUID")
				}
			}
		}

		val note by optionalString {
			name = "note"
			description = "Note explaining what this filter is for"
		}
	}
}
