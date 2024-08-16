/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("StringLiteralDuplication")

package org.quiltmc.community.modes.quilt.extensions

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Guild
import dev.kord.core.event.Event
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.sentry.BreadcrumbType
import dev.kordex.core.utils.hasPermission
import dev.kordex.core.utils.timeoutUntil
import dev.kordex.core.utils.translate
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Instant
import org.quiltmc.community.GUILDS
import org.quiltmc.community.inQuiltGuild
import org.quiltmc.community.notInCollab

private val BAN_PERMS: Array<Permission> = arrayOf(Permission.BanMembers, Permission.Administrator)
private val TIMEOUT_PERMS: Array<Permission> = arrayOf(Permission.ModerateMembers, Permission.Administrator)
private val ROLE_PERMS: Array<Permission> = arrayOf(Permission.ManageRoles, Permission.Administrator)

class SyncExtension : Extension() {
	override val name: String = "sync"

	private val logger = KotlinLogging.logger {}

	private suspend fun <T : Event> CheckContext<T>.hasBanPerms() {
		fail(
			"Must have at least one of these permissions: " + BAN_PERMS.joinToString {
				"**${it.translate(locale)}**"
			}
		)

		BAN_PERMS.forEach {
			val innerCheck = CheckContext(event, locale)
			innerCheck.hasPermission(it)

			if (innerCheck.passed) {
				pass()

				return
			}
		}
	}

	private suspend fun <T : Event> CheckContext<T>.hasBanOrRolePerms() {
		val requiredPerms = (BAN_PERMS + ROLE_PERMS).toSet()

		fail(
			"Must have at least one of these permissions: " + requiredPerms.joinToString { perm ->
				"**${perm.translate(locale)}**"
			}
		)

		requiredPerms.forEach {
			val innerCheck = CheckContext(event, locale)
			innerCheck.hasPermission(it)

			if (innerCheck.passed) {
				pass()

				return
			}
		}
	}

	@Suppress("SpreadOperator")  // No better way atm, and performance impact is negligible
	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "sync"
			description = "Synchronisation commands."

			allowInDms = false

			check { inQuiltGuild() }
			check { hasBanOrRolePerms() }
			check { notInCollab() }

			ephemeralSubCommand {
				name = "bans"
				description = "Additively sync bans between all servers, so that everything matches."

				check { inQuiltGuild() }
				check { hasBanPerms() }
				check { notInCollab() }

				requireBotPermissions(Permission.BanMembers)

				action {
					val guilds = getGuilds()

					logger.info { "Syncing bans for ${guilds.size} guilds." }

					guilds.forEach {
						logger.debug { "${it.id.value} -> ${it.name}" }

						val member = it.getMember(this@SyncExtension.kord.selfId)

						if (!BAN_PERMS.any { perm -> member.hasPermission(perm) }) {
							respond {
								content = "I don't have permission to ban members on ${it.name} (`${it.id.value}`)"
							}

							return@action
						}
					}

					val allBans: MutableMap<Snowflake, String?> = mutableMapOf()
					val syncedBans: MutableMap<Guild, Int> = mutableMapOf()

					guilds.forEach { guild ->
						guild.bans.toList().forEach { ban ->
							if (allBans[ban.userId] == null || ban.reason?.startsWith("Synced:") == false) {
								// If it's null/not present or the given ban entry doesn't start with "Synced:"
								allBans[ban.userId] = ban.reason
							}
						}
					}

					guilds.forEach { guild ->
						allBans.forEach { (userId, reason) ->
							if (guild.getBanOrNull(userId) == null) {
								syncedBans[guild] = (syncedBans[guild] ?: 0) + 1

								guild.ban(userId) {
									this.reason = "Synced: " + (reason ?: "No reason given")
								}
							}
						}
					}

					respond {
						embed {
							title = "Bans synced"

							description = syncedBans.map { "**${it.key.name}**: ${it.value} added" }
								.joinToString("\n")
						}
					}
				}
			}

			ephemeralSubCommand {
				name = "timeouts"
				description = "Additively sync timeouts between all servers, so that everything matches."

				check { inQuiltGuild() }
				check { hasBanPerms() }
				check { notInCollab() }

				requireBotPermissions(Permission.ModerateMembers)

				action {
					val guilds = getGuilds()

					sentry.breadcrumb(BreadcrumbType.Info) {
						message = "Syncing timeouts for ${guilds.size} guilds."
					}

					logger.info { "Syncing timeouts for ${guilds.size} guilds." }

					guilds.forEach {
						logger.debug { "${it.id.value} -> ${it.name}" }

						val member = it.getMember(this@SyncExtension.kord.selfId)

						if (!TIMEOUT_PERMS.any { perm -> member.hasPermission(perm) }) {
							respond {
								content = "I don't have permission to timeout members on ${it.name} (`${it.id.value}`)"
							}

							return@action
						}
					}

					sentry.breadcrumb(BreadcrumbType.Info) {
						message = "Ensured that the bot has adequate permissions on all servers."
					}

					val allTimeouts: MutableMap<Snowflake, Instant> = mutableMapOf()
					val syncedTimeouts: MutableMap<Guild, Int> = mutableMapOf()

					guilds.forEach { guild ->
						sentry.breadcrumb(BreadcrumbType.Info) {
							message = "Collecting timed-out members for guild: ${guild.name} (${guild.id})"
						}

						guild.members
							.filter { it.timeoutUntil != null }
							.collect {
								val current = allTimeouts[it.id]

								if (current == null || current < it.timeoutUntil!!) {
									allTimeouts[it.id] = it.timeoutUntil!!
								}
							}
					}

					sentry.breadcrumb(BreadcrumbType.Info) {
						message = "Collected ${allTimeouts.size} timeouts."
					}

					guilds.forEach { guild ->
						sentry.breadcrumb(BreadcrumbType.Info) {
							message = "Applying up to ${allTimeouts.size} timeouts for guild: ${guild.name} " +
									"(${guild.id})"
						}

						for ((userId, expiry) in allTimeouts) {
							val member = guild.getMemberOrNull(userId) ?: continue

							if (member.timeoutUntil != expiry) {
								member.edit {
									timeoutUntil = expiry

									reason = "Synced automatically"
								}

								syncedTimeouts[guild] = (syncedTimeouts[guild] ?: 0) + 1
							}
						}
					}

					respond {
						embed {
							title = "Timeouts synced"

							description = syncedTimeouts.map { "**${it.key.name}**: ${it.value} added" }
								.joinToString("\n")
						}
					}
				}
			}
		}

		event<BanAddEvent> {
			check { inQuiltGuild() }
			check { notInCollab() }

			action {
				val guilds = getGuilds().filter { it.id != event.guildId }
				val ban = event.getBan()

				guilds.forEach {
					if (it.getBanOrNull(ban.userId) == null) {
						it.ban(ban.userId) {
							this.reason = "Synced: " + (ban.reason ?: "No reason given")
						}
					}
				}
			}
		}

		event<BanRemoveEvent> {
			check { inQuiltGuild() }
			check { notInCollab() }

			action {
				val guilds = getGuilds().filter { it.id != event.guildId }

				guilds.forEach {
					if (it.getBanOrNull(event.user.id) != null) {
						it.unban(event.user.id)
					}
				}
			}
		}

		event<MemberUpdateEvent> {
			check { inQuiltGuild() }
			check { notInCollab() }

			action {
				val guilds = getGuilds().filter { it.id != event.guildId }

				for (guild in guilds) {
					val guildMember = guild.getMemberOrNull(event.member.id) ?: continue

					if (guildMember.timeoutUntil != event.member.timeoutUntil) {
						guildMember.edit {
							timeoutUntil = event.member.timeoutUntil
						}
					}
				}
			}
		}
	}

	private suspend fun getGuilds() = GUILDS.mapNotNull { kord.getGuildOrNull(it) }
}
