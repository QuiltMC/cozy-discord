/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.utils.*
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.UserFlag
import dev.kord.core.Kord
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.threads.ThreadChannelBehavior
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.runBlocking
import org.koin.dsl.bind
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.collections.*
import org.quiltmc.community.database.storage.MongoDBDataAdapter
import org.quiltmc.community.modes.quilt.extensions.settings.SettingsExtension

@Suppress("MagicNumber")  // It's the status code...
suspend fun Kord.getGuildIgnoring403(id: Snowflake) =
	try {
		getGuildOrNull(id)
	} catch (e: RestRequestException) {
		if (e.status.code != 403) {
			throw (e)
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
				single { AmaConfigCollection() } bind AmaConfigCollection::class
				single { FilterCollection() } bind FilterCollection::class
				single { FilterEventCollection() } bind FilterEventCollection::class
				single { GlobalSettingsCollection() } bind GlobalSettingsCollection::class
				single { LinkedMessagesCollection() } bind LinkedMessagesCollection::class
				single { MetaCollection() } bind MetaCollection::class
				single { OwnedThreadCollection() } bind OwnedThreadCollection::class
				single { ServerSettingsCollection() } bind ServerSettingsCollection::class
				single { ServerApplicationCollection() } bind ServerApplicationCollection::class
				single { SuggestionsCollection() } bind SuggestionsCollection::class
				single { TeamCollection() } bind TeamCollection::class
				single { UserFlagsCollection() } bind UserFlagsCollection::class
				single { TagsCollection() } bind TagsCollection::class
				single { WelcomeChannelCollection() } bind WelcomeChannelCollection::class
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
	dataAdapter(::MongoDBDataAdapter)

	applicationCommands {
		// Need to disable this due to the slash command perms experiment
//		syncPermissions = false
	}

	extensions {
		sentry {
			val sentryDsn = envOrNull("SENTRY_DSN")
			val version: String? = object {}::class.java.`package`.implementationVersion

			if (sentryDsn != null) {
				enable = true

				dsn = sentryDsn
			}

			if (version != null) {
				release = version
			}
		}

		help {
			enableBundledExtension = false  // We have no chat commands
		}
	}

	plugins {
		if (ENVIRONMENT != "production") {
			// Add plugin build folders here for testing in dev
			// pluginPath("module-tags/build/libs")
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

suspend fun <C : SlashCommandContext<C, A, M>, A : Arguments, M : ModalForm>
	SlashCommandContext<C, A, M>.getGithubLogChannel(): GuildMessageChannel? {
	val channelId = getKoin().get<GlobalSettingsCollection>().get()?.githubLogChannel ?: return null

	return event.kord.getChannelOf<GuildMessageChannel>(channelId)
}

suspend fun Kord?.getGithubLogChannel(): GuildMessageChannel? {
	val channelId = getKoin().get<GlobalSettingsCollection>().get()?.githubLogChannel ?: return null

	return this?.getChannelOf(channelId)
}

suspend fun Guild.getCozyLogChannel(): GuildMessageChannel? {
	val channelId = getKoin().get<ServerSettingsCollection>().get(id)?.cozyLogChannel ?: return null

	return getChannelOf(channelId)
}

suspend fun Guild.getFilterLogChannel(): GuildMessageChannel? {
	val channelId = getKoin().get<ServerSettingsCollection>().get(id)?.filterLogChannel ?: return null

	return getChannelOf(channelId)
}

suspend fun EmbedBuilder.userField(user: UserBehavior, role: String? = null, inline: Boolean = false) {
	field {
		name = role ?: "User"
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

private const val CHANNEL_NAME_LENGTH = 75

private val THREAD_DELIMITERS = arrayOf(
	",", ".",
	"(", ")",
	"<", ">",
	"[", "]",
)

/**
 * Attempts to generate a thread name based on the message's content.
 *
 * Failing that, it returns a string of format `"$fallbackPrefix | ${message.id}"`
 */
fun Message.contentToThreadName(fallbackPrefix: String): String {
	@Suppress("SpreadOperator")
	return content.trim()
		.split("\n")
		.firstOrNull()
		?.split(*THREAD_DELIMITERS)
		?.firstOrNull()
		?.take(CHANNEL_NAME_LENGTH)

		?: "$fallbackPrefix | $id"
}

@Suppress("DEPRECATION_ERROR") // Either this, or the when block needs an else branch
fun UserFlag.getName(): String = when (this) {
	UserFlag.ActiveDeveloper -> "Active Developer"
	UserFlag.BotHttpInteractions -> "HTTP-Based Commands"
	UserFlag.BugHunterLevel1 -> "Bug Hunter: Level 1"
	UserFlag.BugHunterLevel2 -> "Bug Hunter: Level 2"
	UserFlag.DiscordCertifiedModerator -> "Moderator Programs Alumni"
	UserFlag.DiscordEmployee -> "Discord Employee"
	UserFlag.DiscordPartner -> "Discord Partner"
	UserFlag.EarlySupporter -> "Early Supporter"
	UserFlag.HouseBalance -> "HypeSquad: Balance"
	UserFlag.HouseBravery -> "HypeSquad: Bravery"
	UserFlag.HouseBrilliance -> "HypeSquad: Brilliance"
	UserFlag.HypeSquad -> "HypeSquad"
	UserFlag.TeamUser -> "Team User"
	UserFlag.VerifiedBot -> "Verified Bot"
	UserFlag.VerifiedBotDeveloper -> "Early Verified Bot Developer"
	is UserFlag.Unknown -> "Unknown"
}

fun String.replaceParams(vararg pairs: Pair<String, Any?>): String {
	var result = this

	pairs.forEach {
		result = result.replace(":${it.first}", it.second.toString())
	}

	return result
}

@Suppress("SpreadOperator")
fun String.replaceParams(pairs: Map<String, Any>): String = this.replaceParams(
	*pairs.entries.map { it.toPair() }.toTypedArray()
)

suspend fun ThreadChannelBehavior.getFirstMessage() =
	getMessageOrNull(id)
