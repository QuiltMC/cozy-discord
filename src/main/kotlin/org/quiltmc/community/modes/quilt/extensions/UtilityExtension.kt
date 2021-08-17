@file:OptIn(ExperimentalTime::class, KordPreview::class)

@file:Suppress("MagicNumber")  // Yep. I'm done.

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.checks.isInThread
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.entity.channel.thread.ThreadChannel
import kotlinx.coroutines.flow.toList
import org.koin.core.component.inject
import org.quiltmc.community.COMMUNITY_GUILD
import org.quiltmc.community.COMMUNITY_MANAGEMENT_ROLES
import org.quiltmc.community.database.collections.OwnedThreadCollection
import kotlin.time.ExperimentalTime

class UtilityExtension : Extension() {
    override val name: String = "utility"

    private val threads: OwnedThreadCollection by inject()

    override suspend fun setup() {
        slashCommand(::RenameArguments) {
            name = "rename"
            description = "Rename the current thread, if you have permission"

            guild(COMMUNITY_GUILD)

            check(isInThread)

            action {
                val channel = channel as ThreadChannel
                val member = user.asMember(guild!!.id)
                val roles = member.roles.toList().map { it.id }

                if (COMMUNITY_MANAGEMENT_ROLES.any { it in roles }) {
                    channel.edit {
                        name = arguments.name
                    }

                    ephemeralFollowUp { content = "Thread renamed." }

                    return@action
                }

                if (threads.isOwner(channel, user) != true) {
                    ephemeralFollowUp { content = "This is not your thread." }

                    return@action
                }

                channel.edit {
                    name = arguments.name
                }

                ephemeralFollowUp { content = "Thread renamed." }
            }
        }

        slashCommand(::ArchiveArguments) {
            name = "archive"
            description = "Archive the current thread, if you have permission"

            guild(COMMUNITY_GUILD)

            check(isInThread)

            action {
                val channel = channel as ThreadChannel
                val member = user.asMember(guild!!.id)
                val roles = member.roles.toList().map { it.id }

                if (COMMUNITY_MANAGEMENT_ROLES.any { it in roles }) {
                    channel.edit {
                        this.archived = true
                        this.locked = arguments.lock
                    }

                    ephemeralFollowUp {
                        content = "Thread archived"

                        if (arguments.lock) {
                            content += " and locked"
                        }

                        content += "."
                    }

                    return@action
                }

                if (threads.isOwner(channel, user) != true) {
                    ephemeralFollowUp { content = "This is not your thread." }

                    return@action
                }

                if (channel.isArchived) {
                    ephemeralFollowUp { content = "This channel is already archived." }

                    return@action
                }

                if (arguments.lock) {
                    ephemeralFollowUp { content = "Only members of the community team may lock threads." }

                    return@action
                }

                channel.edit {
                    archived = true
                }

                ephemeralFollowUp { content = "Thread archived." }
            }
        }
    }

    inner class RenameArguments : Arguments() {
        val name by string("name", "Name to give the current thread")
    }

    inner class ArchiveArguments : Arguments() {
        val lock by defaultingBoolean(
            "lock",
            "Whether to lock the thread, if you're staff - defaults to false",
            false
        )
    }
}
