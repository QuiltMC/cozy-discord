/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.embed
import org.koin.core.component.inject
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.database.entities.UserFlags
import org.quiltmc.community.userField

class PKExtension : Extension() {
	override val name: String = "pluralkit"

	private val userFlags: UserFlagsCollection by inject()
	private val settings: ServerSettingsCollection by inject()

	override suspend fun setup() {
		event<ProxiedMessageCreateEvent> {
			action {
				val flags = userFlags.get(event.pkMessage.sender) ?: UserFlags(event.pkMessage.sender, false)

				if (!flags.hasUsedPK) {
					flags.hasUsedPK = true
					flags.save()

					settings.getCommunity()?.getConfiguredLogChannel()?.createMessage {
						embed {
							title = "New PK user"
							color = DISCORD_FUCHSIA

							description = "A message has been sent by a PluralKit user for the first time."

							if (event.pkMessage.system?.avatarUrl != null) {
								thumbnail {
									url = event.pkMessage.system!!.avatarUrl!!
								}
							}

							userField(kord.getUser(event.pkMessage.sender)!!, "Discord Account")

							field {
								name = "Channel"
								value = "${event.message.channel.mention} (`${event.message.channelId}`)"
							}

							field {
								name = "Message URL"
								value = event.message.getJumpUrl()
							}

							field {
								name = "PK Member"

								value = if (event.pkMessage.member != null) {
									"Unknown; private member"
								} else {
									"${event.pkMessage.member?.name} (`${event.pkMessage.member?.id}`)"
								}

								inline = true
							}

							field {
								name = "PK System"

								value = if (event.pkMessage.system != null) {
									"Unknown; private system"
								} else {
									(event.pkMessage.system?.name ?: "**No system name**") +
											" (`${event.pkMessage.system?.id}`)"
								}

								inline = true
							}
						}
					}
				}
			}
		}
	}
}
