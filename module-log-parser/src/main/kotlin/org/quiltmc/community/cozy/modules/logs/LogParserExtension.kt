/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")

package org.quiltmc.community.cozy.modules.logs

import com.charleskorn.kaml.Yaml
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.kotlindiscord.kord.extensions.utils.respond
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import mu.KLogger
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.cozy.modules.logs.config.LogParserConfig
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.PastebinConfig
import org.quiltmc.community.cozy.modules.logs.events.DefaultEventHandler
import org.quiltmc.community.cozy.modules.logs.events.EventHandler
import org.quiltmc.community.cozy.modules.logs.events.PKEventHandler
import java.net.URL
import kotlin.time.Duration.Companion.minutes

public class LogParserExtension : Extension() {
	override val name: String = "quiltmc-log-parser"
	public var scheduler: Scheduler? = null

	private val configUrl: String = envOrNull("PASTEBIN_CONFIG_URL")
		?: "https://raw.githubusercontent.com/QuiltMC/cozy-discord/log-parser/module-log-parser/pastebins.yml"

	private val taskDelay: Long = envOrNull("PASTEBIN_REFRESH_MINS")?.toLong()
		?: 60

	private val config: LogParserConfig by inject()
	private val logger: KLogger = KotlinLogging.logger("org.quiltmc.community.cozy.modules.logs.LogParserExtension")
	private val yaml = Yaml.default

	internal val client: HttpClient = HttpClient(CIO)
	internal lateinit var pastebinConfig: PastebinConfig

	private lateinit var eventHandler: EventHandler

	override suspend fun setup() {
		// TODO: Add commands
		// TODO: Add checks for event handling

		scheduler = Scheduler()
		pastebinConfig = getPastebinConfig()

		scheduler?.schedule(taskDelay.minutes, repeat = true) {
			pastebinConfig = getPastebinConfig()
		}

		eventHandler = if (bot.extensions.containsKey("pluralkit")) {
			PKEventHandler(this)
		} else {
			DefaultEventHandler(this)
		}

		eventHandler.setup()

		config.getRetrievers().forEach { it.setup() }
		config.getProcessors().forEach { it.setup() }
	}

	override suspend fun unload() {
		scheduler?.shutdown()
	}

	internal suspend fun handleMessage(message: Message) {
		if (message.content.isEmpty() && message.attachments.isEmpty()) {
			return
		}

		val logs = (parseLinks(message.content) + message.attachments.map { it.url })
			.map { URL(it) }
			.map { handleLink(it) }
			.flatten()
			.filter { it.aborted || it.hasProblems || it.getMessages().isNotEmpty() }

		if (logs.isNotEmpty()) {
			message.respond(pingInReply = false) {
				addLogs(logs)
			}
		}
	}

	@Suppress("MagicNumber")
	private fun MessageCreateBuilder.addLogs(logs: List<Log>) {
		if (logs.size > 10) {
			content = "**Warning:** I found ${logs.size} logs, but I can't provide results for more than 10 logs at " +
					"a time. You'll only see results for the first 10 logs below - please " +
					"limit the number of logs you post at once."

			logs.forEachIndexed { index, log ->
				embed {
					title = "Parsed Log $index"

					color = if (log.aborted) {
						title += ": Aborted"

						DISCORD_RED
					} else if (log.hasProblems) {
						title += ": Problems Found"

						DISCORD_YELLOW
					} else {
						DISCORD_GREEN
					}

					val header = buildString {
						with(log.environment) {
							appendLine("**__Environment Info__**")
							appendLine("**Java Version:** $javaVersion")
							appendLine("**Java Args:** `$jvmArgs`")
							appendLine("**JVM Version:** $jvmVersion")

							if (glInfo != null) {
								appendLine("**OpenGL Info:** $glInfo")
							}
							if (os != null) {
								appendLine("**OS:** $os")
							}

							appendLine()
						}

						with(log.launcher) {
							if (this != null) {
								appendLine("**$name:** ${version ?: "Unknown version"}")
							}
						}

						appendLine("**Mods:** ${log.getMods().size}")
						appendLine()

						if (log.getLoaders().isNotEmpty()) {
							appendLine("**__Loaders__**")

							log.getLoaders()
								.toList()
								.sortedBy { it.first.name }
								.forEach { (loader, version) ->
									appendLine("**${loader.name.capitalizeWords()}:** $version")
								}

							appendLine()
						}
					}.trim()

					val messages = buildString {
						if (log.aborted) {
							appendLine("__**Log parsing aborted**__")
							appendLine(log.abortReason)
						} else {
							log.getMessages().forEach {
								appendLine(it)
								appendLine()
							}
						}
					}.trim()

					description = (header + "\n\n" + messages)

					if (description!!.length > 4000) {
						description = description!!.take(3994) + "\n[...]"
					}
				}
			}
		}
	}

	@Suppress("TooGenericExceptionCaught")
	private suspend fun handleLink(link: URL): List<Log> {
		val strings: MutableList<String> = mutableListOf()
		val logs: MutableList<Log> = mutableListOf()

		config.getRetrievers().forEach { retriever ->
			try {
				strings.addAll(retriever.process(link))
			} catch (e: Exception) {
				logger.error(e) {
					"Retriever ${retriever.javaClass.simpleName} threw exception for URL: $link"
				}
			}
		}

		strings.forEach { string ->
			val log = Log()

			log.content = string
			log.url = link

			for (parser in config.getParsers()) {
				try {
					parser.process(log)

					if (log.aborted) {
						break
					}
				} catch (e: Exception) {
					logger.error(e) {
						"Parser ${parser.javaClass.simpleName} threw exception for URL: $link"
					}
				}
			}

			for (processor in config.getProcessors()) {
				try {
					processor.process(log)

					if (log.aborted) {
						break
					}
				} catch (e: Exception) {
					logger.error(e) {
						"Processor ${processor.javaClass.simpleName} threw exception for URL: $link"
					}
				}
			}

			logs.add(log)
		}

		return logs
	}

	private suspend fun parseLinks(content: String): Set<String> =
		config.getUrlRegex().findAll(content).map { it.groups[1]!!.value }.toSet()

	private suspend fun getPastebinConfig(): PastebinConfig {
		val text = client.get(configUrl).bodyAsText()

		return yaml.decodeFromString(text)
	}
}
