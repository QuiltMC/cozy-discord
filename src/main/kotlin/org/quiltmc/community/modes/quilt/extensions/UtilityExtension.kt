@file:OptIn(ExperimentalTime::class, KordPreview::class)

@file:Suppress("MagicNumber")  // Yep. I'm done.

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.isInThread
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import org.quiltmc.community.*
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

                if (MODERATOR_ROLES.any { it in roles }) {
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

                if (MODERATOR_ROLES.any { it in roles }) {
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

        slashCommand(::LockArguments) {
            name = "lock"
            description = "Lock a channel, so only moderators can interact in it"

            check(hasBaseModeratorRole)

            action {
                var channelObj = arguments.channel ?: channel

                if (channelObj is ThreadChannel) {
                    channelObj = channelObj.parent.asChannel()
                }

                if (channelObj !is TextChannel) {  // Should never happen, but we handle it for safety
                    ephemeralFollowUp {
                        content = "This command can only be run in a guild text channel."
                    }
                }

                val staffRoleId = when (guild?.id) {
                    COMMUNITY_GUILD -> COMMUNITY_MODERATOR_ROLE
                    TOOLCHAIN_GUILD -> TOOLCHAIN_MODERATOR_ROLE

                    else -> null
                }

                val ch = channelObj as TextChannel

                if (staffRoleId != null) {
                    ch.editRolePermission(staffRoleId) {
                        allowed += Permission.SendMessages
                        allowed += Permission.AddReactions
                        allowed += Permission.UsePublicThreads
                        allowed += Permission.UsePrivateThreads

                        reason = "Channel locked by a moderator."
                    }
                }

                ch.editRolePermission(guild!!.id) {
                    denied += Permission.SendMessages
                    denied += Permission.AddReactions
                    denied += Permission.UsePublicThreads
                    denied += Permission.UsePrivateThreads

                    reason = "Channel locked by a moderator."
                }

                ch.createMessage {
                    content = "Channel locked by a moderator."
                }

                guild?.getModLogChannel()?.createEmbed {
                    title = "Channel locked"
                    color = DISCORD_RED

                    description = "Channel ${ch.mention} was locked by ${user.mention}."
                    timestamp = Clock.System.now()
                }

                ephemeralFollowUp {
                    content = "Channel locked."
                }
            }
        }

        slashCommand(::LockArguments) {
            name = "unlock"
            description = "Unlock a previously locked channel"

            check(hasBaseModeratorRole)

            action {
                var channelObj = arguments.channel ?: channel

                if (channelObj is ThreadChannel) {
                    channelObj = (channelObj as ThreadChannel).parent.asChannel()
                }

                if (channelObj !is TextChannel) {  // Should never happen, but we handle it for safety
                    ephemeralFollowUp {
                        content = "This command can only be run in a guild text channel."
                    }
                }

                val ch = channelObj as TextChannel

                ch.editRolePermission(guild!!.id) {
                    reason = "Channel unlocked by a moderator."
                }

                ch.createMessage {
                    content = "Channel unlocked by a moderator."
                }

                guild?.getModLogChannel()?.createEmbed {
                    title = "Channel unlocked"
                    color = DISCORD_GREEN

                    description = "Channel ${ch.mention} was unlocked by ${user.mention}."
                    timestamp = Clock.System.now()
                }

                ephemeralFollowUp {
                    content = "Channel unlocked."
                }
            }
        }
    }

    suspend fun Guild.getModLogChannel() =
        channels.firstOrNull { it.name == "moderation-log" }
            ?.asChannelOrNull() as? GuildMessageChannel

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

    inner class LockArguments : Arguments() {
        val channel by optionalChannel("channel", "Channel to lock, if not the current one")
    }
}
