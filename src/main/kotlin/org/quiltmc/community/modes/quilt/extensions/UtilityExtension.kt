@file:OptIn(ExperimentalTime::class, KordPreview::class)

@file:Suppress("MagicNumber")  // Yep. I'm done.

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.*
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.checks.isInThread
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.event.channel.thread.TextChannelThreadCreateEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildUpdateEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.*
import org.quiltmc.community.database.collections.OwnedThreadCollection
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

val SPEAKING_PERMISSIONS: Array<Permission> = arrayOf(
    Permission.SendMessages,
    Permission.AddReactions,
    Permission.CreatePublicThreads,
    Permission.CreatePrivateThreads,
    Permission.SendMessagesInThreads,
)

class UtilityExtension : Extension() {
    override val name: String = "utility"

    private val logger = KotlinLogging.logger { }
    private val threads: OwnedThreadCollection by inject()

    private val guildCache: MutableMap<Snowflake, Guild> = mutableMapOf()

    override suspend fun setup() {
        event<TextChannelThreadCreateEvent> {
            check { inQuiltGuild() }
            check { failIf(event.channel.ownerId == kord.selfId) }
            check { failIf(event.channel.member != null) }  // We only want thread creation, not join

            action {
                val owner = event.channel.owner.asUser()

                logger.info { "Thread created by ${owner.tag}" }

                val role = when (event.channel.guildId) {
                    COMMUNITY_GUILD -> event.channel.guild.getRole(COMMUNITY_MODERATOR_ROLE)
                    TOOLCHAIN_GUILD -> event.channel.guild.getRole(TOOLCHAIN_MODERATOR_ROLE)

                    else -> return@action
                }

                val message = event.channel.createMessage {
                    content = "Oh hey, that's a nice thread you've got there! Let me just get the mods in on this " +
                            "sweet discussion..."
                }

                event.channel.withTyping {
                    delay(Duration.Companion.seconds(3))
                }

                message.edit {
                    content = "Hey, ${role.mention}, you've gotta check this thread out!"
                }

                event.channel.withTyping {
                    delay(Duration.Companion.seconds(3))
                }

                message.edit {
                    content = "Welcome to your new thread, ${owner.mention}! This message is at the " +
                            "start of the thread. Remember, you're welcome to use `/archive` and `/rename` at any " +
                            "time!"
                }

                message.pin("First message in the thread.")
            }
        }

        event<GuildCreateEvent> {
            action {
                guildCache[event.guild.id] = event.guild
            }
        }

        event<GuildUpdateEvent> {
            action {
                val new = event.guild
                val old = guildCache[new.id] ?: return@action

                if (old.features != new.features) {
                    val added = new.features - old.features
                    val removed = old.features - new.features

                    if (added.isNotEmpty() && removed.isNotEmpty()) return@action

                    new.getModLogChannel()?.createEmbed {
                        color = DISCORD_FUCHSIA
                        timestamp = Clock.System.now()
                        title = "Guild Flags Updated"

                        description = ""

                        if (added.isNotEmpty()) {
                            description += "**Added:** " + added.joinToString { "`${it.value}`" } + "\n"
                        }

                        if (removed.isNotEmpty()) {
                            description += "**Removed:** " + removed.joinToString { "`${it.value}`" }
                        }
                    }
                }
            }
        }

        GUILDS.forEach { guildId ->
            ephemeralSlashCommand(::SayArguments) {
                name = "say"
                description = "Send a message."

                guild(guildId)

                check { hasBaseModeratorRole() }

                action {
                    val targetChannel = (arguments.target ?: channel.asChannel()) as GuildMessageChannel

                    targetChannel.createMessage(arguments.message)

                    guild?.asGuild()?.getCozyLogChannel()?.createEmbed {
                        title = "/say command used"
                        description = arguments.message

                        field {
                            inline = true
                            name = "Channel"
                            value = targetChannel.mention
                        }

                        field {
                            inline = true
                            name = "User"
                            value = user.mention
                        }
                    }

                    edit { content = "Done!" }
                }
            }

            ephemeralSlashCommand(::MuteRoleArguments) {
                name = "fix-mute-role"
                description = "Fix the permissions for the mute role on this server."

                guild(guildId)

                check { hasPermission(Permission.Administrator) }

                action {
                    val role = arguments.role ?: guild?.asGuild()?.roles?.firstOrNull { it.name.lowercase() == "muted" }

                    if (role == null) {
                        respond {
                            content =
                                "Unable to find a role named `Muted` - double-check the list of roles, or provide " +
                                        "one as an argument."
                        }
                        return@action
                    }

                    var channelsUpdated = 0

                    for (channel in guild!!.channels.toList()) {
                        val overwrite = channel.getPermissionOverwritesForRole(role.id)

                        val allowedPerms = overwrite?.allowed
                        val deniedPerms = overwrite?.denied

                        val hasNonDeniedPerms =
                            deniedPerms == null || SPEAKING_PERMISSIONS.any { !deniedPerms.contains(it) }

                        val canDenyNonAllowedPerms = allowedPerms == null || SPEAKING_PERMISSIONS.any {
                            !allowedPerms.contains(it) && deniedPerms?.contains(it) != true
                        }

                        if (hasNonDeniedPerms && canDenyNonAllowedPerms) {
                            channel.editRolePermission(role.id) {
                                SPEAKING_PERMISSIONS
                                    .filter { allowedPerms?.contains(it) != true }
                                    .forEach { denied += it }

                                SPEAKING_PERMISSIONS
                                    .filter { allowedPerms?.contains(it) == true }
                                    .forEach { allowed += it }

                                reason = "Mute role permissions update triggered by ${user.asUser().tag}"
                            }

                            channelsUpdated += 1
                        }
                    }

                    respond {
                        content = if (channelsUpdated > 0) {
                            "Updated permissions for $channelsUpdated channel/s."
                        } else {
                            "No channels to update."
                        }
                    }

                    val roleUpdated = if (role.permissions.values.isNotEmpty()) {
                        role.edit {
                            permissions = Permissions()

                            reason = "Mute role permissions update triggered by ${user.asUser().tag}"
                        }

                        respond { content = "Mute role permissions cleared." }

                        true
                    } else {
                        respond { content = "Mute role already has no extra permissions." }

                        false
                    }

                    if (channelsUpdated > 0 || roleUpdated) {
                        guild?.asGuildOrNull()?.getModLogChannel()?.createEmbed {
                            title = "Mute role updated"
                            color = DISCORD_BLURPLE

                            description =
                                "Mute role (${role.mention} / `${role.id.asString}`) permissions updated by " +
                                        "${user.mention}."

                            timestamp = Clock.System.now()

                            field {
                                name = "Channels Updated"
                                inline = true

                                value = "$channelsUpdated"
                            }

                            field {
                                name = "Role Updated"
                                inline = true

                                value = if (roleUpdated) "Yes" else "No"
                            }
                        }
                    }
                }
            }

            ephemeralSlashCommand(::RenameArguments) {
                name = "rename"
                description = "Rename the current thread, if you have permission"

                guild(guildId)

                check { isInThread() }

                action {
                    val channel = channel.asChannel() as ThreadChannel
                    val member = user.asMember(guild!!.id)
                    val roles = member.roles.toList().map { it.id }

                    if (MODERATOR_ROLES.any { it in roles }) {
                        channel.edit {
                            name = arguments.name

                            reason = "Renamed by ${member.tag}"
                        }

                        respond { content = "Thread renamed." }

                        return@action
                    }

                    if ((channel.ownerId != user.id && threads.isOwner(channel, user) != true)) {
                        respond { content = "This is not your thread." }

                        return@action
                    }

                    channel.edit {
                        name = arguments.name

                        reason = "Renamed by ${member.tag}"
                    }

                    respond { content = "Thread renamed." }
                }
            }

            ephemeralSlashCommand(::ArchiveArguments) {
                name = "archive"
                description = "Archive the current thread, if you have permission"

                guild(guildId)

                check { isInThread() }

                action {
                    val channel = channel.asChannel() as ThreadChannel
                    val member = user.asMember(guild!!.id)
                    val roles = member.roles.toList().map { it.id }

                    if (MODERATOR_ROLES.any { it in roles }) {
                        channel.edit {
                            this.archived = true
                            this.locked = arguments.lock

                            reason = "Archived by ${user.asUser().tag}"
                        }

                        respond {
                            content = "Thread archived"

                            if (arguments.lock) {
                                content += " and locked"
                            }

                            content += "."
                        }

                        return@action
                    }

                    if (channel.ownerId != user.id && threads.isOwner(channel, user) != true) {
                        respond { content = "This is not your thread." }

                        return@action
                    }

                    if (channel.isArchived) {
                        respond { content = "This channel is already archived." }

                        return@action
                    }

                    if (arguments.lock) {
                        respond { content = "Only members of the community team may lock threads." }

                        return@action
                    }

                    channel.edit {
                        archived = true

                        reason = "Archived by ${user.asUser().tag}"
                    }

                    respond { content = "Thread archived." }
                }
            }

            ephemeralMessageCommand {
                name = "Pin in thread"

                guild(guildId)

                check { isInThread() }

                action {
                    val channel = channel.asChannel() as ThreadChannel
                    val member = user.asMember(guild!!.id)
                    val roles = member.roles.toList().map { it.id }

                    if (MODERATOR_ROLES.any { it in roles }) {
                        targetMessages.forEach { it.pin("Pinned by ${member.tag}") }
                        edit { content = "Messages pinned." }

                        return@action
                    }

                    if (channel.ownerId != user.id && threads.isOwner(channel, user) != true) {
                        respond { content = "This is not your thread." }

                        return@action
                    }

                    targetMessages.forEach { it.pin("Pinned by ${member.tag}") }

                    edit { content = "Messages pinned." }
                }
            }

            ephemeralMessageCommand {
                name = "Unpin in thread"

                guild(guildId)

                check { isInThread() }

                action {
                    val channel = channel.asChannel() as ThreadChannel
                    val member = user.asMember(guild!!.id)
                    val roles = member.roles.toList().map { it.id }

                    if (MODERATOR_ROLES.any { it in roles }) {
                        targetMessages.forEach { it.unpin("Unpinned by ${member.tag}") }
                        edit { content = "Messages unpinned." }

                        return@action
                    }

                    if (channel.ownerId != user.id && threads.isOwner(channel, user) != true) {
                        respond { content = "This is not your thread." }

                        return@action
                    }

                    targetMessages.forEach { it.unpin("Unpinned by ${member.tag}") }

                    edit { content = "Messages unpinned." }
                }
            }

            ephemeralSlashCommand {
                name = "lock-server"
                description = "Lock the server, preventing anyone but staff from talking"

                guild(guildId)

                check { hasPermission(Permission.Administrator) }

                action {
                    val roleId = when (guild!!.id) {
                        COMMUNITY_GUILD -> COMMUNITY_MODERATOR_ROLE
                        TOOLCHAIN_GUILD -> TOOLCHAIN_MODERATOR_ROLE

                        else -> throw DiscordRelayedException("Incorrect server ID: ${guild?.id?.value}")
                    }

                    val moderatorRole = guild!!.getRole(roleId)
                    val everyoneRole = guild!!.getRole(guild!!.id)

                    everyoneRole.edit {
                        permissions = everyoneRole.permissions
                            .minus(Permission.AddReactions)
                            .minus(Permission.CreatePrivateThreads)
                            .minus(Permission.CreatePublicThreads)
                            .minus(Permission.SendMessages)
                            .minus(Permission.SendMessagesInThreads)

                        reason = "Server locked down by ${user.asUser().tag}"
                    }

                    moderatorRole.edit {
                        permissions = moderatorRole.permissions
                            .plus(Permission.AddReactions)
                            .plus(Permission.CreatePrivateThreads)
                            .plus(Permission.CreatePublicThreads)
                            .plus(Permission.SendMessages)
                            .plus(Permission.SendMessagesInThreads)

                        reason = "Server locked down by ${user.asUser().tag}"
                    }

                    guild?.asGuildOrNull()?.getModLogChannel()?.createEmbed {
                        title = "Server locked"
                        color = DISCORD_RED

                        description = "Server was locked by ${user.mention}."
                        timestamp = Clock.System.now()
                    }

                    respond {
                        content = "Server locked."
                    }
                }
            }

            ephemeralSlashCommand {
                name = "unlock-server"
                description = "Unlock the server, allowing users to talk again"

                guild(guildId)

                check { hasPermission(Permission.Administrator) }

                action {
                    val roleId = when (guild!!.id) {
                        COMMUNITY_GUILD -> COMMUNITY_MODERATOR_ROLE
                        TOOLCHAIN_GUILD -> TOOLCHAIN_MODERATOR_ROLE

                        else -> throw DiscordRelayedException("Incorrect server ID: ${guild?.id?.value}")
                    }

                    val moderatorRole = guild!!.getRole(roleId)
                    val everyoneRole = guild!!.getRole(guild!!.id)

                    everyoneRole.edit {
                        permissions = everyoneRole.permissions
                            .plus(Permission.AddReactions)
                            .plus(Permission.CreatePrivateThreads)
                            .plus(Permission.CreatePublicThreads)
                            .plus(Permission.SendMessages)
                            .plus(Permission.SendMessagesInThreads)

                        reason = "Server unlocked by ${user.asUser().tag}"
                    }

                    moderatorRole.edit {
                        permissions = moderatorRole.permissions
                            .minus(Permission.AddReactions)
                            .minus(Permission.CreatePrivateThreads)
                            .minus(Permission.CreatePublicThreads)
                            .minus(Permission.SendMessages)
                            .minus(Permission.SendMessagesInThreads)

                        reason = "Server unlocked by ${user.asUser().tag}"
                    }

                    guild?.asGuildOrNull()?.getModLogChannel()?.createEmbed {
                        title = "Server unlocked"
                        color = DISCORD_GREEN

                        description = "Server was unlocked by ${user.mention}."
                        timestamp = Clock.System.now()
                    }

                    respond {
                        content = "Server unlocked."
                    }
                }
            }

            ephemeralSlashCommand(::LockArguments) {
                name = "lock"
                description = "Lock a channel, so only moderators can interact in it"

                guild(guildId)

                check { hasBaseModeratorRole() }

                action {
                    var channelObj = arguments.channel ?: channel.asChannel()

                    if (channelObj is ThreadChannel) {
                        channelObj = channelObj.parent.asChannel()
                    }

                    if (channelObj !is TextChannel) {  // Should never happen, but we handle it for safety
                        respond {
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
                            SPEAKING_PERMISSIONS.forEach { allowed += it }

                            reason = "Channel locked by ${user.asUser().tag}"
                        }
                    }

                    ch.editRolePermission(guild!!.id) {
                        SPEAKING_PERMISSIONS.forEach { denied += it }

                        reason = "Channel locked by ${user.asUser().tag}"
                    }

                    ch.createMessage {
                        content = "Channel locked by a moderator."
                    }

                    guild?.asGuildOrNull()?.getModLogChannel()?.createEmbed {
                        title = "Channel locked"
                        color = DISCORD_RED

                        description = "Channel ${ch.mention} was locked by ${user.mention}."
                        timestamp = Clock.System.now()
                    }

                    respond {
                        content = "Channel locked."
                    }
                }
            }

            ephemeralSlashCommand(::LockArguments) {
                name = "unlock"
                description = "Unlock a previously locked channel"

                guild(guildId)

                check { hasBaseModeratorRole() }

                action {
                    var channelObj = arguments.channel ?: channel.asChannel()

                    if (channelObj is ThreadChannel) {
                        channelObj = (channelObj as ThreadChannel).parent.asChannel()
                    }

                    if (channelObj !is TextChannel) {  // Should never happen, but we handle it for safety
                        respond {
                            content = "This command can only be run in a guild text channel."
                        }
                    }

                    val ch = channelObj as TextChannel

                    ch.getPermissionOverwritesForRole(guild!!.id)
                        ?.delete("Channel unlocked by ${user.asUser().tag}")

                    ch.createMessage {
                        content = "Channel unlocked by a moderator."
                    }

                    guild?.asGuildOrNull()?.getModLogChannel()?.createEmbed {
                        title = "Channel unlocked"
                        color = DISCORD_GREEN

                        description = "Channel ${ch.mention} was unlocked by ${user.mention}."
                        timestamp = Clock.System.now()
                    }

                    respond {
                        content = "Channel unlocked."
                    }
                }
            }
        }
    }

    suspend fun Guild.getModLogChannel() =
        channels.firstOrNull { it.name == "moderation-log" }
            ?.asChannelOrNull() as? GuildMessageChannel

    suspend fun Guild.getCozyLogChannel() =
        channels.firstOrNull { it.name == "cozy-logs" }
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

    inner class MuteRoleArguments : Arguments() {
        val role by optionalRole("role", "Mute role ID, if the role isn't named Muted")
    }

    inner class SayArguments : Arguments() {
        val message by string("message", "Message to send")
        val target by optionalChannel("target", "Channel to use, if not this one") { _, value ->
            if (value != null && value !is GuildMessageChannel) {
                throw DiscordRelayedException("${value.mention} is not a guild text channel.")
            }
        }
    }
}
