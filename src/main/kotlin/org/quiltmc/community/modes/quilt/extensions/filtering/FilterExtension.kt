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
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.*
import org.quiltmc.community.database.collections.FilterCollection
import org.quiltmc.community.database.collections.FilterEventCollection
import org.quiltmc.community.database.entities.FilterEntry
import java.util.*

const val APPEALS_INVITE_CODE = "H32HVWw9Nu"
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

class FilterExtension : Extension() {
    override val name: String = "filter"
    private val logger = KotlinLogging.logger { }

    private val rgxProperties = RgxGenProperties()
    private val inviteCache: MutableMap<String, Snowflake> = mutableMapOf()

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

        GUILDS.forEach { guildId ->
            ephemeralSlashCommand {
                name = "filters"
                description = "Filter management commands"

                check { hasBaseModeratorRole() }

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

                        filter.action = arguments.action
                        filters.set(filter)

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

                        filter.note = arguments.note
                        filters.set(filter)

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
                        val filter = FilterEntry(
                            _id = UUID.randomUUID(),

                            action = arguments.action,
                            pingStaff = arguments.ping,

                            match = arguments.match,
                            matchType = arguments.matchType,

                            note = arguments.note
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
            .filter { it.matches(message.content) }
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

                            "If you'd like to appeal your ban: https://discord.gg/$APPEALS_INVITE_CODE"

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
        MatchType.CONTAINS -> content.contains(match, ignoreCase = true)
        MatchType.EXACT -> content.equals(match, ignoreCase = true)
        MatchType.REGEX -> match.toRegex(RegexOption.IGNORE_CASE).matches(content)
        MatchType.REGEX_CONTAINS -> content.contains(match.toRegex(RegexOption.IGNORE_CASE))
        MatchType.INVITE -> content.containsInviteFor(Snowflake(match))
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

            val invite = kord.getInvite(code, false)
            val inviteGuild = invite?.partialGuild?.id ?: continue

            inviteCache[code] = inviteGuild

            if (inviteGuild == guild) {
                return true
            }
        }

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
