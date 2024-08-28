/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.minecraft

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_FUCHSIA
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.checks.or
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.message
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.pagination.pages.Page
import dev.kordex.core.utils.scheduling.Scheduler
import dev.kordex.core.utils.scheduling.Task
import dev.kordex.core.utils.toReaction
import dev.kordex.parser.Cursor
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.apache.commons.text.StringEscapeUtils
import org.quiltmc.community.*
import kotlin.system.exitProcess

private const val PAGINATOR_TIMEOUT = 60_000L  // One minute
private const val CHUNK_SIZE = 10

private const val BASE_URL = "https://launchercontent.mojang.com/v2"
private const val JSON_URL = "$BASE_URL/javaPatchNotes.json"

private const val CHECK_DELAY = 60L

private val LINK_REGEX = "<a href=\"?(?<url>[^\"\\s]+)\"?[^>]*>(?<text>[^<]+)</a>".toRegex()

@Suppress("MagicNumber", "UnderscoresInNumericLiterals")
private val CHANNELS: List<Snowflake> = listOf(
//	Snowflake(1220660330191917056L),  // Testing
	Snowflake(838805249271267398L),  // Community
//	Snowflake(834195264629243904L),  // Toolchain
)

class MinecraftExtension : Extension() {
	override val name: String = "minecraft"

	private val logger = KotlinLogging.logger { }

	private val client = HttpClient {
		install(ContentNegotiation) {
			json(
				Json {
					ignoreUnknownKeys = true
				}
			)
		}

		expectSuccess = true
	}

	private val scheduler = Scheduler()

	private var checkTask: Task? = null
	private var knownVersions: MutableSet<String> = mutableSetOf()
	private lateinit var currentEntries: PatchNoteEntries

	@OptIn(KordPreview::class)
	override suspend fun setup() {
		populateVersions()

		checkTask = scheduler.schedule(CHECK_DELAY, callback = ::checkTask)

		for (guildId in GUILDS + COLLAB_GUILD) {
			ephemeralSlashCommand {
				name = "mc"
				description = "Commands related to Minecraft updates"

				allowInDms = false

				guild(guildId)

				ephemeralSubCommand(::CheckArguments) {
					name = "get"
					description = "Retrieve the patch notes for a given Minecraft version, or the latest if not " +
						"supplied."

					action {
						if (!::currentEntries.isInitialized) {
							respond { content = "Still setting up - try again a bit later!" }
							return@action
						}

						val patch = if (arguments.version == null) {
							currentEntries.entries.first()
						} else {
							currentEntries.entries.firstOrNull { it.version.equals(arguments.version, true) }
						}

						if (patch == null) {
							respond { content = "Unknown version supplied: `${arguments.version}`" }
							return@action
						}

						respond {
							patchNotes(patch.get())
						}
					}
				}

				ephemeralSubCommand {
					name = "versions"
					description = "Get a list of patch note versions."

					action {
						if (!::currentEntries.isInitialized) {
							respond { content = "Still setting up - try again a bit later!" }

							return@action
						}

						editingPaginator {
							timeoutSeconds = PAGINATOR_TIMEOUT

							knownVersions.chunked(CHUNK_SIZE).forEach { chunk ->
								page(
									Page {
										title = "Patch note versions"
										color = DISCORD_FUCHSIA

										description = chunk.joinToString("\n") { "**»** `$it`" }

										footer {
											text = "${currentEntries.entries.size} versions"
										}
									}
								)
							}
						}.send()
					}
				}

				ephemeralSubCommand(::CheckArguments) {
					name = "forget"
					description = "Forget a version (the last one by default), allowing it to be relayed again."

					check {
						hasBaseModeratorRole()

						or {
							hasPermission(Permission.Administrator)
						}
					}

					action {
						if (!::currentEntries.isInitialized) {
							respond { content = "Still setting up - try again a bit later!" }
							return@action
						}

						val version = if (arguments.version == null) {
							currentEntries.entries.first().version
						} else {
							currentEntries.entries.firstOrNull {
								it.version.equals(arguments.version, true)
							}?.version
						}

						if (version == null) {
							respond { content = "Unknown version supplied: `${arguments.version}`" }
							return@action
						}

						knownVersions.remove(version)

						respond { content = "Version forgotten: `$version`" }
					}
				}

				ephemeralSubCommand(::UpdateArguments) {
					name = "update"
					description = "Edit the given message to replace its embed. Useful when formatting code changes."

					check {
						hasBaseModeratorRole()

						or {
							hasPermission(Permission.Administrator)
						}
					}

					action {
						if (!::currentEntries.isInitialized) {
							respond { content = "Still setting up - try again a bit later!" }
							return@action
						}

						val entry = currentEntries.entries.firstOrNull {
							it.version.equals(arguments.version, true)
						}

						if (entry == null) {
							respond { content = "Unknown version supplied: `${arguments.version}`" }
							return@action
						}

						arguments.message.edit {
							patchNotes(entry.get())
						}

						respond { content = "Message edit to match version: `${entry.version}`" }
					}
				}

				ephemeralSubCommand {
					name = "run"
					description = "Run the check task now, without waiting for it."

					check {
						hasBaseModeratorRole()

						or {
							hasPermission(Permission.Administrator)
						}
					}

					action {
						respond { content = "Checking now..." }

						checkTask?.callNow()
					}
				}
			}
		}
	}

	suspend fun populateVersions() {
		currentEntries = client.get(JSON_URL).body()

		currentEntries.entries.forEach { knownVersions.add(it.version) }
	}

	@Suppress("TooGenericExceptionCaught")
	suspend fun checkTask() {
		try {
			val now = Clock.System.now()

			currentEntries = client.get(JSON_URL + "?cbt=${now.epochSeconds}").body()

			currentEntries.entries.forEach {
				if (it.version !in knownVersions) {
					relayUpdate(it.get())
					knownVersions.add(it.version)
				}
			}
		} catch (t: Throwable) {
			logger.error(t) { "Check task run failed" }
		} finally {
			checkTask = scheduler.schedule(CHECK_DELAY, callback = ::checkTask)
		}
	}

	@Suppress("TooGenericExceptionCaught")
	suspend fun relayUpdate(patchNote: PatchNote) =
		CHANNELS
			.map {
				try {
					kord.getChannelOf<TopGuildMessageChannel>(it)
				} catch (t: Throwable) {
					logger.warn(t) { "Unable to get channel of ID: ${it.value}" }

					null
				}
			}
			.filterNotNull()
			.forEach { it.relay(patchNote) }

	fun String.formatHTML(): String {
		var result = StringEscapeUtils.unescapeHtml4(trim('\n'))

		result = result.replace("\u200B", "")
		result = result.replace("<p></p>", "")

		result = result.replace("<hr/?>".toRegex(), "\\_\\_\\_\\_\\_\\_\\_\\_\\_\\_\\_\\_")
		result = result.replace("</hr>", "")

		result = result.replace("[\n]*</p>\n+<p>[\n]*".toRegex(), "\n\n")
		result = result.replace("[\n]*<[/]*p>[\n]*".toRegex(), "\n")

		result = result.replace("<strong>", "**")
		result = result.replace("</strong>", "**")

		result = result.replace("<em>", "_")
		result = result.replace("</em>", "_")

		result = result.replace("<code>", "`")
		result = result.replace("</code>", "`")

		@Suppress("MagicNumber")
		for (i in 1..6) {
			result = result.replace("[\n]*<h$i>[\n]*".toRegex(), "\n\n${"#".repeat(i)} ")
			result = result.replace("[\n]*</h$i>[\n]*".toRegex(), "\n")
		}

		result = result.replace("[\n]*<[ou]l>[\n]*".toRegex(), "\n\n")
		result = result.replace("[\n]*</[ou]l>[\n]*".toRegex(), "\n\n")

		result = result.replace("[\n]*</li>\n+<li>[\n]*".toRegex(), "\n- ")
		result = result.replace("([\n]{2,})?<li>[\n]*".toRegex(), "\n- ")
		result = result.replace("[\n]*</li>[\n]*".toRegex(), "\n\n")

		val links = LINK_REGEX.findAll(result)

		links.forEach {
			result = result.replace(
				it.value,
				"[${it.groups["text"]?.value}](${it.groups["url"]?.value})"
			)
		}

		val cursor = Cursor(result)
		var isQuote = false

		result = ""

		@Suppress("LoopWithTooManyJumpStatements")  // Nah.
		while (cursor.hasNext) {
			result = result + (
				cursor.consumeWhile { it != '<' }?.prefixQuote(isQuote)
					?: break
				)

			val temp = cursor.consumeWhile { it != '>' }
				?.plus(cursor.nextOrNull() ?: "")
				?: break

			if (temp == "<blockquote>") {
				isQuote = true

				if (cursor.peekNext() == '\n') {
					cursor.next()
				}

				continue
			} else if (temp == "</blockquote>") {
				isQuote = false

				continue
			}

			result = result + temp.prefixQuote(isQuote)
		}

		result = result.replace("&#60", "<")

		return result.trim()
	}

	fun String.prefixQuote(prefix: Boolean) =
		if (prefix) {
			split("\n")
				.joinToString("\n") {
					"> $it"
				}
		} else {
			this
		}

	fun String.truncateMarkdown(maxLength: Int = 3000): Pair<String, Int> {
		var result = this

		if (length > maxLength) {
			val truncated = result.substring(0, maxLength).substringBeforeLast("\n")
			val remaining = result.substringAfter(truncated).count { it == '\n' }

			result = truncated

			return result to remaining
		}

		return result to 0
	}

	private fun MessageBuilder.patchNotes(patchNote: PatchNote, maxLength: Int = 3000) {
		val (truncated, remaining) = patchNote.body.formatHTML().truncateMarkdown(maxLength)

		actionRow {
			linkButton("https://quiltmc.org/mc-patchnotes/#${patchNote.version}") {
				label = "Read more..."

				emoji("🔗".toReaction() as ReactionEmoji.Unicode)
			}
		}

		embed {
			title = patchNote.title
			color = DISCORD_GREEN

			description = truncated

			if (remaining > 0) {
				description += "\n\n[... $remaining more lines]"
			}

			thumbnail {
				url = "$BASE_URL${patchNote.image.url}"
			}

			footer {
				text = "URL: https://quiltmc.org/mc-patchnotes/#${patchNote.version}"
			}
		}
	}

	private suspend fun TopGuildMessageChannel.relay(patchNote: PatchNote) {
		val message = createMessage {
			// If we are in the community guild, ping the update role
			if (guildId == COMMUNITY_GUILD) {
				content = "<@&$MINECRAFT_UPDATE_PING_ROLE>"
			}

			patchNotes(patchNote)
		}

		val title = if (patchNote.title.startsWith("minecraft ", true)) {
			patchNote.title.split(" ", limit = 2).last()
		} else {
			patchNote.title
		}

		if (guildId == COMMUNITY_GUILD) {
			when (this) {
				is TextChannel -> startPublicThreadWithMessage(
					message.id, title
				) { reason = "Thread created for Minecraft update" }

				is NewsChannel -> {
					startPublicThreadWithMessage(
						message.id, title
					) { reason = "Thread created for Minecraft update" }

					message.publish()
				}
			}
		}
	}

	fun getLatest() =
		currentEntries.entries.first()

	suspend fun PatchNoteEntry.get() =
		client.get("$BASE_URL/$contentPath").body<PatchNote>()

	@OptIn(KordPreview::class)
	class CheckArguments : Arguments() {
		val version by optionalString {
			name = "version"
			description = "Specific version to get patch notes for"
		}
	}

	@OptIn(KordPreview::class)
	class UpdateArguments : Arguments() {
		val version by string {
			name = "version"
			description = "Specific version to get patch notes for"
		}

		val message by message {
			name = "message"
			description = "Message to edit with a new embed"
		}
	}
}

// In-dev testing function
@Suppress("unused")
private suspend fun main() {
	val ext = MinecraftExtension()
	ext.populateVersions()

	val current = ext.getLatest()

	with(ext) {
		println(current.get().body.formatHTML())
	}

	exitProcess(0)
}
