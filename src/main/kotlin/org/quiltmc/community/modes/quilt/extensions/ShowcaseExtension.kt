/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTime::class)

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.checks.inChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.authorId
import dev.kord.common.entity.MessageType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.delay
import org.koin.core.component.inject
import org.quiltmc.community.*
import org.quiltmc.community.database.collections.OwnedThreadCollection
import org.quiltmc.community.database.entities.OwnedThread
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private val THREAD_DELAY = 3.seconds
private const val CHANNEL_NAME_LENGTH = 75

class ShowcaseExtension : Extension() {
    override val name: String = "showcase"

    private val threads: OwnedThreadCollection by inject()

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check {
                failIfNot {
                    event.message.type == MessageType.Default ||
                            event.message.type == MessageType.Reply
                }
            }

            check { failIf(event.message.data.authorId == event.kord.selfId) }
            check { failIf(event.message.author == null) }
            check { failIf(event.message.author?.isBot == true) }
            check { failIf(event.message.content.trim().isEmpty()) }
            check { failIf(event.message.interaction != null) }

            check { inChannel(GALLERY_CHANNEL) }

            action {
                // TODO: This sort of thing *needs* to be factored out into a broader threads manager

                val guild = event.message.getGuild()

                val role = when (guild.id) {
                    COMMUNITY_GUILD -> guild.getRole(COMMUNITY_MODERATOR_ROLE)
                    TOOLCHAIN_GUILD -> guild.getRole(TOOLCHAIN_MODERATOR_ROLE)

                    else -> return@action
                }

                val author = event.message.author!!
                val channel = event.message.channel.asChannelOf<TextChannel>()

                val title = event.message.content.trim()
                    .split("\n")
                    .firstOrNull()
                    ?.split(",", ".")
                    ?.firstOrNull()
                    ?.take(CHANNEL_NAME_LENGTH)

                    ?: "Gallery | ${event.message.id}"

                val thread = channel.startPublicThreadWithMessage(
                    event.message.id,
                    title
                )

                threads.set(
                    OwnedThread(thread.id, author.id, guild.id)
                )

                val message = thread.createMessage {
                    content = "Oh hey, that's a nice gallery post you've got there! Let me just get the mods in on " +
                            "this sweet showcase..."
                }

                message.pin("First message in the thread.")

                thread.withTyping {
                    delay(THREAD_DELAY)
                }

                message.edit {
                    content = "Hey, ${role.mention}, you've gotta check this showcase out!"
                }

                thread.withTyping {
                    delay(THREAD_DELAY)
                }

                message.edit {
                    content = "Welcome to your new gallery thread, ${author.mention}! This message is at the " +
                            "start of the thread. Remember, you're welcome to use the `/thread` commands to manage " +
                            "your thread as needed.\n\n" +

                            "We recommend using `/thread rename` to give your thread a more meaningful title if the " +
                            "generated one isn't good enough!\n\n" +

                            "**Note:** To avoid filling up the sidebar, this thread has been archived on creation. " +
                            "Feel free to send a message or rename it to unarchive it!"
                }

                thread.edit {
                    archived = true
                    reason = "Gallery thread archived on creation."
                }
            }
        }
    }
}
