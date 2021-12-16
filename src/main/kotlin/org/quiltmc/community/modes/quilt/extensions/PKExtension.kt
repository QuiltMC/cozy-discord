package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.delay
import org.koin.core.component.inject
import org.quiltmc.community.api.pluralkit.PluralKit
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.database.entities.UserFlags
import org.quiltmc.community.userField

private const val PK_DELAY_MILLIS: Long = 500

class PKExtension : Extension() {
    override val name: String = "pluralkit"

    private val pluralKit = PluralKit()

    private val userFlags: UserFlagsCollection by inject()
    private val settings: ServerSettingsCollection by inject()

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { failIf(event.message.data.webhookId.value == null) }
            check { failIf(event.message.interaction != null) }

            action {
                delay(PK_DELAY_MILLIS)  // To allow the PK API to catch up

                val pkMessage = pluralKit.getMessageOrNull(event.message.id) ?: return@action
                val flags = userFlags.get(pkMessage.sender) ?: UserFlags(pkMessage.sender, false)

                if (!flags.hasUsedPK) {
                    flags.hasUsedPK = true
                    flags.save()

                    settings.getCommunity()?.getConfiguredLogChannel()?.createMessage {
                        embed {
                            title = "New PK user"
                            color = DISCORD_FUCHSIA

                            description = "A message has been sent by a PluralKit user for the first time."

                            userField(kord.getUser(pkMessage.sender)!!, "Discord Account")

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

                                value = "${pkMessage.member.name} (`${pkMessage.system.id}` / " +
                                        "`${pkMessage.system.uuid}`)"

                                inline = true
                            }

                            field {
                                name = "PK System"

                                value = (pkMessage.system.name ?: "**No system name**") +
                                        " (`${pkMessage.system.id}` / `${pkMessage.system.uuid}`)"

                                inline = true
                            }
                        }
                    }
                }
            }
        }
    }
}
