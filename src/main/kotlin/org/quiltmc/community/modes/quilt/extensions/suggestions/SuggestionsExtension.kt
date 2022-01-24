@file:OptIn(ExperimentalTime::class, KordPreview::class)

@file:Suppress("MagicNumber")  // Yep. I'm done.

package org.quiltmc.community.modes.quilt.extensions.suggestions

import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.checks.inChannel
import com.kotlindiscord.kord.extensions.checks.inTopChannel
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.optionalEnumChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.*
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.behavior.reply
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import org.koin.core.component.inject
import org.quiltmc.community.*
import org.quiltmc.community.api.pluralkit.PluralKit
import org.quiltmc.community.database.collections.OwnedThreadCollection
import org.quiltmc.community.database.collections.SuggestionsCollection
import org.quiltmc.community.database.entities.OwnedThread
import org.quiltmc.community.database.entities.Suggestion
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private const val ACTION_DOWN = "down"
private const val ACTION_REMOVE = "remove"
private const val ACTION_UP = "up"

private const val THREAD_INTRO = "This message is at the top of the thread.\n\n" +
        "If this is your suggestion, **please** use `/thread rename` to change the " +
        "name of the thread! You're also welcome to use the other `/thread` commands to manage " +
        "your suggestion thread as needed. You can edit your suggestion at any time using the `/edit-suggestion`" +
        " command."

private const val COMMENT_SIZE_LIMIT: Long = 800
private const val SUGGESTION_SIZE_LIMIT: Long = 1000
private const val THIRTY_SECONDS: Long = 30_000
private const val TUPPERBOX_DELAY: Long = 5

private const val GITHUB_EMOJI: String = "<:github:864972399111569468>"

private val EMOTE_DOWNVOTE = ReactionEmoji.Unicode("⬇️")
private val EMOTE_REMOVE = ReactionEmoji.Unicode("\uD83D\uDDD1️")
private val EMOTE_UPVOTE = ReactionEmoji.Unicode("⬆️")

private val CLEAR_WORDS = arrayOf("clear", "null")

class SuggestionsExtension : Extension() {
    override val name: String = "suggestions"

    private val suggestions: SuggestionsCollection by inject()
    private val threads: OwnedThreadCollection by inject()

    private val pluralKit = PluralKit()

    override suspend fun setup() {
        // region: Events

        event<MessageCreateEvent> {
            check {
                failIfNot {
                    event.message.type == MessageType.Default ||
                            event.message.type == MessageType.Reply
                }
            }

            check { failIf(event.message.data.authorId == event.kord.selfId) }
            check { failIf(event.message.author?.isBot == true) }
            check { failIf(event.message.content.trim().isEmpty()) }
            check { failIf(event.message.interaction != null) }

            check { inChannel(SUGGESTION_CHANNEL) }

            action {
                event.message.channel.withTyping {
                    delay(Duration.seconds(TUPPERBOX_DELAY))

                    // If it's been yeeted, it's probably been moderated or proxied
                    event.message.channel.getMessageOrNull(event.message.id) ?: return@action
                }

                val id = event.message.id

                val suggestion = if (event.message.webhookId != null) {
                    val pkMessage = pluralKit.getMessageOrNull(event.message.id)

                    if (pkMessage != null) {
                        Suggestion(
                            _id = id,
                            text = event.message.content,

                            owner = pkMessage.sender,
                            ownerAvatar = "https://cdn.discordapp.com/avatars/" +
                                    "${event.message.data.author.id.value}/" +
                                    "${event.message.data.author.avatar}.webp",
                            ownerName = event.message.data.author.username,

                            positiveVoters = mutableListOf(pkMessage.sender),

                            isTupper = true
                        )
                    } else {
                        null
                    }
                } else {
                    val pkMessage = pluralKit.getMessageOrNull(event.message.id)

                    if (pkMessage == null) {
                        Suggestion(
                            _id = id,
                            text = event.message.content,

                            owner = event.message.author!!.id,
                            ownerAvatar = event.message.author!!.avatar?.url,
                            ownerName = event.message.author!!.asMember(event.message.getGuild().id).displayName,

                            positiveVoters = mutableListOf(event.message.author!!.id)
                        )
                    } else {
                        null
                    }
                } ?: return@action

                if (suggestion.text.length > SUGGESTION_SIZE_LIMIT) {
                    val user = kord.getUser(suggestion.owner)

                    val resentText = if (suggestion.text.length > 1800) {
                        suggestion.text.substring(0, 1797) + "..."
                    } else {
                        suggestion.text
                    }

                    val errorMessage = "The suggestion you posted was too long (${suggestion.text.length} / " +
                            "$SUGGESTION_SIZE_LIMIT characters)\n\n```\n$resentText\n```"

                    val dm = user?.dm {
                        content = errorMessage
                    }

                    if (dm != null) {
                        event.message.delete()
                    } else {
                        event.message.reply {
                            content = errorMessage
                        }.delete(THIRTY_SECONDS)

                        event.message.delete(THIRTY_SECONDS)
                    }

                    return@action
                }

                suggestions.set(suggestion)
                sendSuggestion(suggestion)
                event.message.delete()
            }
        }

        event<MessageCreateEvent> {
            check { failIfNot(event.message.channelId == SUGGESTION_CHANNEL) }
            check { failIfNot(event.message.type == MessageType.ThreadCreated) }

            action {
                event.message.deleteIgnoringNotFound()
            }
        }

        event<InteractionCreateEvent> {
            check { failIfNot(event.interaction is ButtonInteraction) }
            check { inTopChannel(SUGGESTION_CHANNEL) }

            action {
                val interaction = event.interaction as ButtonInteraction

                if ("/" !in interaction.componentId) {
                    return@action
                }

                val split = interaction.componentId.split('/', limit = 2)

                val id = Snowflake(split[0])
                val action = split[1]

                val suggestion = suggestions.get(id) ?: return@action
                val response = interaction.ackEphemeral(false)

                if (suggestion.status != SuggestionStatus.Open) {
                    response.followUpEphemeral {
                        content = "**Error:** This suggestion isn't open, and votes can't be changed."
                    }

                    return@action
                }

                when (action) {
                    ACTION_UP -> if (!suggestion.positiveVoters.contains(interaction.user.id)) {
                        suggestion.positiveVoters.add(interaction.user.id)
                        suggestion.negativeVoters.remove(interaction.user.id)

                        response.followUpEphemeral {
                            content = "Vote registered!"
                        }
                    } else {
                        response.followUpEphemeral {
                            content = "**Error:** You've already upvoted this suggestion."
                        }

                        return@action
                    }

                    ACTION_DOWN -> if (!suggestion.negativeVoters.contains(interaction.user.id)) {
                        suggestion.negativeVoters.add(interaction.user.id)
                        suggestion.positiveVoters.remove(interaction.user.id)

                        response.followUpEphemeral {
                            content = "Vote registered!"
                        }
                    } else {
                        response.followUpEphemeral {
                            content = "**Error:** You've already downvoted this suggestion."
                        }

                        return@action
                    }

                    ACTION_REMOVE -> if (suggestion.positiveVoters.contains(interaction.user.id)) {
                        suggestion.positiveVoters.remove(interaction.user.id)

                        response.followUpEphemeral {
                            content = "Vote removed!"
                        }
                    } else if (suggestion.negativeVoters.contains(interaction.user.id)) {
                        suggestion.negativeVoters.remove(interaction.user.id)

                        response.followUpEphemeral {
                            content = "Vote removed!"
                        }
                    } else {
                        response.followUpEphemeral {
                            content = "**Error:** You haven't voted for this suggestion."
                        }

                        return@action
                    }

                    else -> response.followUpEphemeral {
                        content = "Unknown action: $action"

                        return@action
                    }
                }

                suggestions.set(suggestion)
                sendSuggestion(suggestion)
            }
        }

        event<ThreadChannelCreateEvent> {
            check { inTopChannel(SUGGESTION_CHANNEL) }

            check { failIf(event.channel.ownerId == kord.selfId) }

            action {
                event.channel.delete("Suggestion thread not created by Cozy")

                event.channel.owner.asUser().dm {
                    content = "I've removed your thread - please note that suggestion threads are only " +
                            "meant to be created automatically, you shouldn't create your own."
                }
            }
        }

        // endregion

        // region: Commands

        ephemeralSlashCommand(::SuggestionEditArguments) {
            name = "edit-suggestion"
            description = "Edit one of your suggestions"

            guild(COMMUNITY_GUILD)

            action {
                if (arguments.suggestion.owner != user.id) {
                    respond {
                        content = "**Error:** You don't own that suggestion."
                    }

                    return@action
                }

                arguments.suggestion.text = arguments.text

                suggestions.set(arguments.suggestion)
                sendSuggestion(arguments.suggestion)

                respond {
                    content = "Suggestion updated."
                }
            }
        }

        ephemeralSlashCommand(::SuggestionStateArguments) {
            name = "suggestion"
            description = "Suggestion state change command; \"clear\" to remove comment"

            guild(COMMUNITY_GUILD)

            MODERATOR_ROLES.forEach(::allowRole)

            check { hasRole(COMMUNITY_MODERATOR_ROLE) }

            action {
                val status = arguments.status

                if (status != null) {
                    arguments.suggestion.status = status
                }

                if (arguments.comment != null) {
                    arguments.suggestion.comment = if (arguments.comment!!.lowercase() in CLEAR_WORDS) {
                        null
                    } else {
                        arguments.comment
                    }
                }

                if (arguments.issue != null) {
                    arguments.suggestion.githubIssue = if (arguments.issue!!.lowercase() in CLEAR_WORDS) {
                        null
                    } else {
                        arguments.issue
                    }
                }

                suggestions.set(arguments.suggestion)
                sendSuggestion(arguments.suggestion)
                sendSuggestionUpdateMessage(arguments.suggestion)

                respond {
                    content = "Suggestion updated."
                }
            }
        }

        // TODO: Searching command?
//            subCommand(::SuggestionSearchArguments) {
//                name = "search"
//                description = "Search through the submitted suggestions"
//
//                COMMUNITY_MANAGEMENT_ROLES.forEach(::allowRole)
//
//                action {
//
//                }
//            }

        // endregion
    }

    suspend fun sendSuggestion(suggestion: Suggestion) {
        val channel = getChannel()

        if (suggestion.message == null) {
            val message = channel.createMessage { suggestion(suggestion) }

            val thread = (channel as? TextChannel)?.startPublicThreadWithMessage(
                message.id,
                name = suggestion._id.toString(),
                archiveDuration = channel.guild.asGuild().getMaxArchiveDuration()
            )

            if (thread != null) {
                val threadMessage = thread.createMessage {
                    suggestion(suggestion, sendEmbed = false)

                    content = THREAD_INTRO
                }

                threadMessage.pin()

                thread.addUser(suggestion.owner)

                threads.set(
                    OwnedThread(
                        thread.id,
                        suggestion.owner,
                        thread.guildId
                    )
                )

                suggestion.thread = thread.id
                suggestion.threadButtons = threadMessage.id

                val modRole = when (thread.guildId) {
                    COMMUNITY_GUILD -> thread.guild.getRole(COMMUNITY_MODERATOR_ROLE)
                    TOOLCHAIN_GUILD -> thread.guild.getRole(TOOLCHAIN_MODERATOR_ROLE)

                    else -> return
                }

                val pingMessage = thread.createMessage {
                    content = "Oh right, better get the mods in..."
                }

                delay(Duration.seconds(3))

                pingMessage.edit {
                    content = "Oh right, better get the mods in...\n" +
                            "Hey, ${modRole.mention}! Squirrel!"
                }

                delay(Duration.seconds(3))

                pingMessage.delete("Removing temporary moderator ping message.")
            }

            suggestion.message = message.id

            suggestions.set(suggestion)
        } else {
            val message = channel.getMessage(suggestion.message!!)

            message.edit { suggestion(suggestion) }

            if (suggestion.thread != null && suggestion.threadButtons != null) {
                val thread = (channel as? TextChannel)?.activeThreads?.toList()?.firstOrNull {
                    it.id == suggestion.thread
                }

                val threadMessage = thread?.getMessage(suggestion.threadButtons!!)

                threadMessage?.edit {
                    suggestion(suggestion, false)

                    content = THREAD_INTRO
                }
            }
        }
    }

    suspend fun sendSuggestionUpdateMessage(suggestion: Suggestion) {
        val user = kord.getUser(suggestion.owner) ?: return

        val suggestionMessage = if (suggestion.message != null) {
            kord.getGuild(COMMUNITY_GUILD)
                ?.getChannelOf<GuildMessageChannel>(SUGGESTION_CHANNEL)
                ?.getMessageOrNull(suggestion.message!!)
        } else {
            null
        }

        user.dm {
            embed {
                color = suggestion.status.color
                title = "Suggestion updated"

                description = if (suggestionMessage != null) {
                    "[Suggestion ${suggestion._id.value}](${suggestionMessage.getJumpUrl()}) "
                } else {
                    "Suggestion ${suggestion._id.value} "
                }

                description += "has been updated.\n\n" +
                        "**__Suggestion__**\n\n" +
                        suggestion.text

                description += "\n\n**Status:** ${suggestion.status.readableName}\n"

                if (suggestion.githubIssue != null) {
                    val issue = suggestion.githubIssue!!
                    val (repoName, issueNumber) = issue.split('/')

                    description += "$GITHUB_EMOJI  " +
                            "[$repoName#$issueNumber]" +
                            "(https://github.com/QuiltMC/$repoName/issues/$issueNumber)\n"
                }

                if (suggestion.comment != null) {
                    description += "\n" +
                            "**__Staff response__**\n\n" +
                            suggestion.comment
                }
            }
        }

        if (suggestion.thread != null) {
            kord.getChannelOf<ThreadChannel>(suggestion.thread!!)?.createMessage {
                content = "**__Suggestion updated__**\n" +
                        "**Status:** ${suggestion.status.readableName}\n"

                if (suggestion.githubIssue != null) {
                    val issue = suggestion.githubIssue!!
                    val (repoName, issueNumber) = issue.split('/')

                    content += "$GITHUB_EMOJI  " +
                            "<https://github.com/QuiltMC/$repoName/issues/$issueNumber>\n"
                }

                if (suggestion.comment != null) {
                    content += "\n" +
                            "**__Staff response__**\n\n" +
                            suggestion.comment
                }
            }
        }
    }

    suspend fun getChannel() = kord.getChannelOf<GuildMessageChannel>(SUGGESTION_CHANNEL)!!

    fun MessageCreateBuilder.suggestion(suggestion: Suggestion, sendEmbed: Boolean = true) {
        val id = suggestion._id.value

        if (sendEmbed) {
            embed {
                author {
                    name = suggestion.ownerName
                    icon = suggestion.ownerAvatar
                }

                description = if (suggestion.isTupper) {
                    "@${suggestion.ownerName} (<@${suggestion.owner.value}>)\n\n"
                } else {
                    "<@${suggestion.owner.value}>\n\n"
                }

                description += "${suggestion.text}\n\n"

                if (suggestion.githubIssue != null) {
                    val issue = suggestion.githubIssue!!
                    val (repoName, issueNumber) = issue.split('/')

                    description += "$GITHUB_EMOJI " +
                            "[$repoName#$issueNumber]" +
                            "(https://github.com/QuiltMC/$repoName/issues/$issueNumber)\n\n"
                }

                if (suggestion.positiveVotes > 0) {
                    description += "**Upvotes:** ${suggestion.positiveVotes}\n"
                }

                if (suggestion.negativeVotes > 0) {
                    description += "**Downvotes:** ${suggestion.negativeVotes}\n"
                }

                description += "**Total:** ${suggestion.voteDifference}"

                if (suggestion.comment != null) {
                    description += "\n\n**__Staff response__\n\n** ${suggestion.comment}"
                }

                color = suggestion.status.color

                footer {
                    text = "Status: ${suggestion.status.readableName} • ID: $id"
                }
            }
        }

        if (suggestion.status == SuggestionStatus.Open) {
            actionRow {
                interactionButton(ButtonStyle.Primary, "$id/$ACTION_UP") {
                    emoji(EMOTE_UPVOTE)

                    label = "Upvote"
                }

                interactionButton(ButtonStyle.Primary, "$id/$ACTION_DOWN") {
                    emoji(EMOTE_DOWNVOTE)

                    label = "Downvote"
                }

                interactionButton(ButtonStyle.Danger, "$id/$ACTION_REMOVE") {
                    emoji(EMOTE_REMOVE)

                    label = "Retract vote"
                }
            }
        }
    }

    fun MessageModifyBuilder.suggestion(suggestion: Suggestion, sendEmbed: Boolean = true) {
        val id = suggestion._id.value

        if (sendEmbed) {
            embed {
                author {
                    name = suggestion.ownerName
                    icon = suggestion.ownerAvatar
                }

                description = if (suggestion.isTupper) {
                    "@${suggestion.ownerName} (<@${suggestion.owner.value}>)\n\n"
                } else {
                    "<@${suggestion.owner.value}>\n\n"
                }

                description += "${suggestion.text}\n\n"

                if (suggestion.githubIssue != null) {
                    val issue = suggestion.githubIssue!!
                    val (repoName, issueNumber) = issue.split('/')

                    description += "$GITHUB_EMOJI " +
                            "[$repoName#$issueNumber]" +
                            "(https://github.com/QuiltMC/$repoName/issues/$issueNumber)\n\n"
                }

                if (suggestion.positiveVotes > 0) {
                    description += "**Upvotes:** ${suggestion.positiveVotes}\n"
                }

                if (suggestion.negativeVotes > 0) {
                    description += "**Downvotes:** ${suggestion.negativeVotes}\n"
                }

                description += "**Total:** ${suggestion.voteDifference}"

                if (suggestion.comment != null) {
                    description += "\n\n**__Staff response__\n\n** ${suggestion.comment}"
                }

                color = suggestion.status.color

                footer {
                    text = "Status: ${suggestion.status.readableName} • ID: $id"
                }
            }
        }

        if (suggestion.status == SuggestionStatus.Open) {
            actionRow {
                interactionButton(ButtonStyle.Primary, "$id/$ACTION_UP") {
                    emoji(EMOTE_UPVOTE)

                    label = "Upvote"
                }

                interactionButton(ButtonStyle.Primary, "$id/$ACTION_DOWN") {
                    emoji(EMOTE_DOWNVOTE)

                    label = "Downvote"
                }

                interactionButton(ButtonStyle.Danger, "$id/$ACTION_REMOVE") {
                    emoji(EMOTE_REMOVE)

                    label = "Retract vote"
                }
            }
        } else if (suggestion.status != SuggestionStatus.Open) {
            components = mutableListOf()
        }
    }

    inner class SuggestionEditArguments : Arguments() {
        val suggestion by suggestion {
            name = "suggestion"
            description = "Suggestion ID to act on"
        }

        val text by coalescingString {
            name = "text"
            description = "New suggestion text"

            validate {
                if (value.length > SUGGESTION_SIZE_LIMIT) {
                    fail("Suggestion text must not be longer than $SUGGESTION_SIZE_LIMIT characters.")
                }
            }
        }
    }

//    inner class SuggestionSearchArguments : Arguments() {
//        val status by defaultingEnumChoice<SuggestionStatus>(
//            "status",
//            "Status to check for, defaulting to Approved",
//            "Status",
//            SuggestionStatus.Approved
//        )
//
//        val sentiment by optionalEnumChoice<SuggestionSentiment>(
//            "sentiment",
//            "How the community voted",
//            "Sentiment"
//        )
//
//        val user by optionalUser("user", "Suggestion creator")
//        val suggestion by optionalSuggestion("suggestion", "Suggestion ID to search for")
//
//        val text by optionalCoalescingString("text", "Text to search for in the description")
//    }

    inner class SuggestionStateArguments : Arguments() {
        val suggestion by suggestion {
            name = "suggestion"
            description = "Suggestion ID to act on"
        }

        val status by optionalEnumChoice<SuggestionStatus> {
            name = "status"
            description = "Status to apply"

            typeName = "status"
        }

        val comment by optionalString {
            name = "comment"
            description = "Comment text to set, 'clear' to remove"

            validate {
                if ((value?.length ?: -1) > COMMENT_SIZE_LIMIT) {
                    fail("Comment must not be longer than $COMMENT_SIZE_LIMIT characters.")
                }
            }
        }

        val issue by optionalString {
            name = "github-issue"
            description = "GitHub issue for this suggestion, 'clear' to remove"

            validate {
                value ?: return@validate

                failIf(
                    "Issue specification must be of the form `repo/123`, without the repo owner - " +
                            "For example: `cozy-discord/12`"
                ) { value!!.count { it == '/' } != 1 }

                if (passed) {
                    failIf(
                        "Issue numbers must be integers"
                    ) { value!!.split('/').last().toIntOrNull() == null }
                }
            }
        }
    }
}
