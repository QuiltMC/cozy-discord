/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(KordUnsafe::class, KordExperimental::class)

package org.quiltmc.community

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.kotlindiscord.kord.extensions.*
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.soywiz.korio.async.launch
import dev.kord.common.Color
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.WebhookBehavior
import dev.kord.core.behavior.execute
import dev.kord.rest.builder.message.embed
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import org.koin.core.component.inject

class DiscordLogAppender : AppenderBase<ILoggingEvent>(), KordExKoinComponent {
	lateinit var url: String
	var level: Level = Level.ALL

	private val webhookId: Snowflake by lazy {
		val parts = url.split("/").toMutableList()

		parts.removeLast()
		Snowflake(parts.removeLast())
	}

	private val webhookToken: String by lazy {
		val parts = url.split("/").toMutableList()
		parts.removeLast()
	}

	private val webhook: WebhookBehavior by lazy {
		kord.unsafe.webhook(webhookId)
	}

	private val logger = KotlinLogging.logger("org.quiltmc.community.DiscordLogAppender")
	private val kord: Kord by inject()

	@Suppress("TooGenericExceptionCaught")
	override fun append(eventObject: ILoggingEvent) {
		if (!eventObject.level.isGreaterOrEqual(level)) {
			return
		}

		kord.launch {
			try {
				webhook.execute(webhookToken) {
					embed {
						description = eventObject.message
						timestamp = Instant.fromEpochMilliseconds(eventObject.timeStamp)
						title = "Log message: ${eventObject.level.levelStr}"

						color = when (eventObject.level.levelInt) {
							Level.ERROR_INT -> DISCORD_RED
							Level.WARN_INT -> DISCORD_YELLOW
							Level.INFO_INT -> DISCORD_BLURPLE
							Level.DEBUG_INT -> DISCORD_WHITE
							Level.TRACE_INT -> DISCORD_BLACK

							else -> Color(0, 0, 0)
						}

						field {
							name = "Logger"
							value = "`${eventObject.loggerName}`"
						}

						field {
							name = "Thread name"
							value = "`${eventObject.threadName}`"
						}
					}
				}
			} catch (e: Exception) {
				logger.error(e) { "Failed to log message to Discord." }
			}
		}
	}
}
