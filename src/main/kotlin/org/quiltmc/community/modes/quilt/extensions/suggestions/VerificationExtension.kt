/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.suggestions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.inQuiltGuild
import kotlin.time.Duration.Companion.minutes

class VerificationExtension : Extension() {
	override val name: String = "verification"

	private val logger = KotlinLogging.logger { }
	private val serverSettings: ServerSettingsCollection by inject()

	override suspend fun setup() {
		event<GuildCreateEvent> {
			check { inQuiltGuild() }

			action {
				val roleId = serverSettings.get(event.guild.id)?.verificationRole
				val guild = event.guild

				if (roleId == null) {
					logger.debug { "Guild ${guild.name} (${guild.id}) has no verification role set." }

					return@action
				}

				logger.debug { "Waiting for a minute before syncing verification roles..." }

				delay(1.minutes)

				guild.members
					.filter { !it.isPending && roleId !in it.roleIds }
					.collect {
						it.addRole(roleId, "Member has passed screening")
						logger.debug { "Verification role applied to user: ${it.id}" }
					}
			}
		}

		event<MemberUpdateEvent> {
			check { inQuiltGuild() }
			check { failIf(event.member.isPending, "Member is pending") }

			action {
				val roleId = serverSettings.get(event.guildId)?.verificationRole

				if (roleId == null) {
					val guild = event.guild.asGuild()

					logger.debug { "Guild ${guild.name} (${guild.id}) has no verification role set." }

					return@action
				}

				if (event.guild.getRoleOrNull(roleId) == null) {
					val guild = event.guild.asGuild()

					logger.warn {
						"Guild ${guild.name} (${guild.id}) has a verification role set, but the role has been deleted."
					}

					return@action
				}

				if (!event.member.isPending && roleId !in event.member.roleIds) {
					event.member.addRole(roleId, "Member has passed screening")

					logger.debug { "Verification role applied to user: ${event.member.id}" }
				}
			}
		}
	}
}
