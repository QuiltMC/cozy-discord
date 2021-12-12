package org.quiltmc.community

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.kotlindiscord.kord.extensions.utils.getKoin
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.runBlocking
import org.koin.dsl.bind
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.collections.*
import org.quiltmc.community.database.getSettings
import org.quiltmc.community.modes.quilt.extensions.settings.SettingsExtension

@Suppress("MagicNumber")  // It's the status code...
suspend fun Kord.getGuildIgnoring403(id: Snowflake) =
    try {
        getGuild(id)
    } catch (e: RestRequestException) {
        if (e.status.code != 403) {
            throw(e)
        }

        null
    }

fun String.chunkByWhitespace(length: Int): List<String> {
    if (length <= 0) {
        error("Length must be greater than 0")
    }

    if (contains("\n")) {
        error("String must be a single line")
    }

    val words = split(" ")
    var currentLine = ""
    val lines: MutableList<String> = mutableListOf()

    for (word in words) {
        if (word.length >= length) {
            val parts = word.chunked(length)

            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = ""
            }

            parts.forEach {
                if (it.length == length) {
                    lines.add(it)
                } else if (it.isNotEmpty()) {
                    currentLine = it
                }
            }
        } else {
            val newLength = currentLine.length + word.length + if (currentLine.isEmpty()) 0 else 1

            if (newLength > length) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine += if (currentLine.isEmpty()) word else " $word"
            }
        }
    }

    if (currentLine.isNotEmpty()) {
        lines.add(currentLine)
    }

    return lines
}

suspend fun ExtensibleBotBuilder.database(migrate: Boolean = false) {
    val url = env("DB_URL")
    val db = Database(url)

    hooks {
        beforeKoinSetup {
            loadModule {
                single { db } bind Database::class
            }

            loadModule {
                single { FilterCollection() } bind FilterCollection::class
                single { FilterEventCollection() } bind FilterEventCollection::class
                single { GlobalSettingsCollection() } bind GlobalSettingsCollection::class
                single { MetaCollection() } bind MetaCollection::class
                single { OwnedThreadCollection() } bind OwnedThreadCollection::class
                single { ServerSettingsCollection() } bind ServerSettingsCollection::class
                single { SuggestionsCollection() } bind SuggestionsCollection::class
                single { TeamCollection() } bind TeamCollection::class
            }

            if (migrate) {
                runBlocking {
                    db.migrate()
                }
            }
        }
    }
}

suspend fun ExtensibleBotBuilder.common() {
    chatCommands {
        defaultPrefix = "?"

        prefix { default ->
            getGuild()?.getSettings()?.commandPrefix ?: default
        }

        check {
            if (event.message.author == null) {
                fail()
            }
        }
    }

    extensions {
        add(::SettingsExtension)

        sentry {
            val sentryDsn = envOrNull("SENTRY_DSN")

            if (sentryDsn != null) {
                enable = true

                dsn = sentryDsn
            }
        }
    }
}

suspend fun ExtensibleBotBuilder.settings() {
    extensions {
        add(::SettingsExtension)
    }
}

fun Guild.getMaxArchiveDuration(): ArchiveDuration {
    val features = features.filter {
        it.value == "THREE_DAY_THREAD_ARCHIVE" ||
                it.value == "SEVEN_DAY_THREAD_ARCHIVE"
    }.map { it.value }

    return when {
        features.contains("SEVEN_DAY_THREAD_ARCHIVE") -> ArchiveDuration.Week
        features.contains("THREE_DAY_THREAD_ARCHIVE") -> ArchiveDuration.ThreeDays

        else -> ArchiveDuration.Day
    }
}

// Logging-related extensions

suspend fun <C : SlashCommandContext<C, A>, A : Arguments>
        SlashCommandContext<C, A>.getGithubLogChannel(): GuildMessageChannel? {
    val channelId = getKoin().get<GlobalSettingsCollection>().get()?.githubLogChannel ?: return null

    return event.kord.getChannelOf<GuildMessageChannel>(channelId)
}

suspend fun Kord?.getGithubLogChannel(): GuildMessageChannel? {
    val channelId = getKoin().get<GlobalSettingsCollection>().get()?.githubLogChannel ?: return null

    return this?.getChannelOf<GuildMessageChannel>(channelId)
}

suspend fun EmbedBuilder.userField(user: UserBehavior, role: String, inline: Boolean = false) {
    field {
        name = role
        value = "${user.mention} (`${user.id}` / `${user.asUser().tag}`)"

        this.inline = inline
    }
}

fun EmbedBuilder.channelField(channel: MessageChannelBehavior, title: String, inline: Boolean = false) {
    field {
        this.name = title
        this.value = "${channel.mention} (`${channel.id}`)"

        this.inline = inline
    }
}
