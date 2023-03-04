/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.events.extra.GuildJoinRequestDeleteEvent
import com.kotlindiscord.kord.extensions.events.extra.GuildJoinRequestUpdateEvent
import com.kotlindiscord.kord.extensions.events.extra.models.GuildJoinRequestResponse
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import mu.KotlinLogging
import org.quiltmc.community.inQuiltGuild

class ApplicationsExtension : Extension() {
	override val name: String = "applications"
	private val logger = KotlinLogging.logger { }

	override suspend fun setup() {
		event<GuildJoinRequestUpdateEvent> {
			check { inQuiltGuild() }

			action {
				logger.info {
					buildString {
						appendLine("== GUILD REQUEST CREATED ==")
						appendLine()
						appendLine("  Status: ${event.status.name}")
						appendLine()
						appendLine("  Actioned at: ${event.request.actionedAt}")
						appendLine("  Created at: ${event.request.createdAt}")
						appendLine()
						appendLine("  Guild ID: ${event.guildId}")
						appendLine("  User ID: ${event.userId}")
						appendLine("  Request ID: ${event.requestId}")
						appendLine()
						appendLine("  == RESPONSES ==")
						appendLine()

						event.request.formResponses.forEach {
							append("${it.label}: ")

							when (it) {
								is GuildJoinRequestResponse.TermsResponse ->
									append(
										if (it.response) {
											"Accepted"
										} else {
											"Not accepted"
										}
									)

								is GuildJoinRequestResponse.MultipleChoiceResponse ->
									append(it.choices[it.response])

								is GuildJoinRequestResponse.ParagraphResponse -> {
									appendLine()
									appendLine(it.response.prependIndent("  "))
									appendLine()
								}
							}

							appendLine()
						}
					}
				}
			}
		}

		event<GuildJoinRequestDeleteEvent> {
			check { inQuiltGuild() }

			action {
				logger.info {
					buildString {
						appendLine("== GUILD REQUEST DELETED ==")
						appendLine()
						appendLine("  Guild ID: ${event.guildId}")
						appendLine("  User ID: ${event.userId}")
						appendLine("  Request ID: ${event.requestId}")
					}
				}
			}
		}
	}
}
