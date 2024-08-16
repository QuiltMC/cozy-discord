/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")

package org.quiltmc.community.cozy.modules.logs

import com.charleskorn.kaml.Yaml
import dev.kord.core.entity.Message
import dev.kord.core.event.Event
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.capitalizeWords
import dev.kordex.core.utils.envOrNull
import dev.kordex.core.utils.respond
import dev.kordex.core.utils.scheduling.Scheduler
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import org.koin.core.component.inject
import org.quiltmc.community.cozy.modules.logs.config.LogParserConfig
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.PastebinConfig
import org.quiltmc.community.cozy.modules.logs.events.DefaultEventHandler
import org.quiltmc.community.cozy.modules.logs.events.EventHandler
import org.quiltmc.community.cozy.modules.logs.events.PKEventHandler
import org.quiltmc.community.cozy.modules.logs.types.BaseLogHandler
import java.net.URI
import java.net.URL
import kotlin.time.Duration.Companion.minutes

public class LogParserExtension : Extension() {
	override val name: String = "quiltmc-log-parser"

	private var scheduler: Scheduler? = null

	private val configUrl: String = envOrNull("PASTEBIN_CONFIG_URL")
		?: "https://raw.githubusercontent.com/QuiltMC/cozy-discord/root/module-log-parser/pastebins.yml"

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

		eventHandler = if (bot.extensions.containsKey("ext-pluralkit")) {
			logger.info { "Loading PluralKit-based event handlers" }

			PKEventHandler(this)
		} else {
			logger.info { "Loading default event handlers, without PluralKit support" }

			DefaultEventHandler(this)
		}

		eventHandler.setup()

		config.getRetrievers().forEach { it.setup() }
		config.getProcessors().forEach { it.setup() }
	}

	override suspend fun unload() {
		scheduler?.shutdown()
	}

	internal suspend fun handleMessage(message: Message, event: Event) {
		if (message.content.isEmpty() && message.attachments.isEmpty()) {
			return
		}

		val logs = (parseLinks(message.content) + message.attachments.map { it.url })
			.map { URI(it).toURL() }
			.map { handleLink(it, event) }
			.flatten()
			.filter {
				it.aborted ||
					it.hasProblems ||
					it.getMessages().isNotEmpty() ||
					it.minecraftVersion != null ||
					it.getMods().isNotEmpty()
			}

//			.filter { it.aborted || it.hasProblems || it.getMessages().isNotEmpty() }

		if (logs.isNotEmpty()) {
			message.respond(pingInReply = false) {
				addLogs(logs)
			}
		}
	}

	@Suppress("MagicNumber")
	private suspend fun MessageCreateBuilder.addLogs(logs: List<Log>) {
		if (logs.size > 10) {
			content = "**Warning:** I found ${logs.size} logs, but I can't provide results for more than 10 logs at " +
				"a time. You'll only see results for the first 10 logs below - please " +
				"limit the number of logs you post at once."
		}

		logs.forEach { log ->
			embed {
				title = if (log.content.startsWith("---- Crashed! ----")) {
					"Crash Log"
				} else {
					"Log File"
				}

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
						val mcVersion = log.minecraftVersion?.string ?: "Unknown"

						appendLine("**__Environment Info__**")
						appendLine()
						appendLine("**Minecraft Version:** `$mcVersion`")

						var addAnotherLine = false

						if (javaVersion != null) {
							appendLine("**Java Version:** `$javaVersion`")

							addAnotherLine = true
						}

						if (jvmVersion != null) {
							appendLine("**JVM Version:** `$jvmVersion`")

							addAnotherLine = true
						}

						if (addAnotherLine) {
							addAnotherLine = false

							appendLine()
						}

						if (jvmArgs != null) {
							appendLine("**Java Args:** `$jvmArgs`")

							addAnotherLine = true
						}

						if (addAnotherLine) {
							addAnotherLine = false

							appendLine()
						}

						if (os != null) {
							appendLine("**OS:** $os")

							addAnotherLine = true
						}

						if (cpu != null) {
							appendLine("**CPU:** `$cpu`")

							addAnotherLine = true
						}

						if (gpu != null) {
							appendLine("**GPU:** `$gpu`")

							addAnotherLine = true
						}

						if (systemMemory != null) {
							appendLine("**System Memory:** `$systemMemory`")

							addAnotherLine = true
						}

						if (addAnotherLine) {
							addAnotherLine = false

							appendLine()
						}

						if (gameMemory != null) {
							appendLine("**Game Memory:** `$gameMemory`")

							addAnotherLine = true
						}

						if (shaderpack != null) {
							appendLine("**Shaderpack:** `$shaderpack`")

							addAnotherLine = true
						}

						if (addAnotherLine) {
							appendLine()
						}
					}

					with(log.launcher) {
						if (this != null) {
							appendLine("**Launcher:** $name (`${version ?: "Unknown Version"}`)")
							appendLine()
						}
					}

					if (log.getLoaders().isNotEmpty()) {
						log.getLoaders()
							.toList()
							.sortedBy { it.first.name }
							.forEach { (loader, version) ->
								appendLine("**Loader:** ${loader.name.capitalizeWords()} (`${version.string}`)")
							}
					}

					appendLine(
						"**Mods:** " + if (log.getMods().isNotEmpty()) {
							log.getMods().size
						} else {
							"None"
						}
					)

					appendLine()
				}.trim()

				if (log.aborted || log.getMessages().isNotEmpty()) {
					val messages = buildString {
						appendLine("__**Messages**__")
						appendLine()

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

					description = "$header\n\n$messages"
				} else {
					description = header
				}

				if (description!!.length > 4000) {
					description = description!!.take(3994) + "\n[...]"
				}
			}

			log.extraEmbeds.forEach {
				embed { it(this) }
			}
		}

		if (embeds == null) embeds = mutableListOf()
		val embeds = embeds!!

		if (embeds.size > 10) {
			val extraEmbeds = embeds.size - 10
			val allEmbeds = embeds.take(10)

			embeds.clear()
			embeds.addAll(allEmbeds)

			if (content == null) {
				content = ""
			} else {
				content += "\n\n"
			}

			content += "**Warning:** $extraEmbeds extra embeds were generated when parsing your logs. Please fix the " +
				"issues that have been detailed here, and try again with new logs. Alternatively, if you submitted " +
				"more than one log in the same message, you could try submitting them one at a time."
		}
	}

	@Suppress("TooGenericExceptionCaught")
	private suspend fun handleLink(link: URL, event: Event): List<Log> {
		val strings: MutableList<String> = mutableListOf()
		val logs: MutableList<Log> = mutableListOf()

		for (retriever in config.getRetrievers()) {
			if (!checkPredicates(retriever, event) || !retriever._predicate(link, event)) {
				continue
			}

			try {
				strings.addAll(retriever.process(link).map { it.replace("\r\n", "\n") })
			} catch (e: Exception) {
				logger.error(e) {
					"Retriever ${retriever.identifier} threw exception for URL: $link"
				}
			}
		}

		strings.forEach { string ->
			val log = Log()

			log.content = string
			log.url = link

			for (parser in config.getParsers()) {
				if (!checkPredicates(parser, event) || !parser._predicate(log, event)) {
					continue
				}

				try {
					parser.process(log)

					if (log.aborted) {
						break
					}
				} catch (e: Exception) {
					logger.error(e) {
						"Parser ${parser.identifier} threw exception for URL: $link"
					}
				}
			}

			for (processor in config.getProcessors()) {
				if (!checkPredicates(processor, event) || !processor._predicate(log, event)) {
					continue
				}

				try {
					processor.process(log)

					if (log.aborted) {
						break
					}
				} catch (e: Exception) {
					logger.error(e) {
						"Processor ${processor.identifier} threw exception for URL: $link"
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

	private suspend fun checkPredicates(handler: BaseLogHandler, event: Event) =
		config.getGlobalPredicates().all { it(handler, event) }
}
