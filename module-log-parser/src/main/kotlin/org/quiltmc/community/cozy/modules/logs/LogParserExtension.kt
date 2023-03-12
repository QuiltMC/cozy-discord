/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")

package org.quiltmc.community.cozy.modules.logs

import com.charleskorn.kaml.Yaml
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import dev.kord.core.event.message.MessageCreateEvent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import org.quiltmc.community.cozy.modules.logs.data.PastebinConfig
import kotlin.time.Duration.Companion.minutes

public class LogParserExtension : Extension() {
	override val name: String = "quiltmc-log-parser"
	public var scheduler: Scheduler? = null

	private val configUrl: String = envOrNull("PASTEBIN_CONFIG_URL")
		?: "https://raw.githubusercontent.com/QuiltMC/cozy-discord/root/module-log-parser/pastebins.yml"

	private val taskDelay: Long = envOrNull("PASTEBIN_REFRESH_MINS")?.toLong()
		?: 60

	private val yaml = Yaml()

	internal val client: HttpClient = HttpClient(CIO)
	internal lateinit var pastebinConfig: PastebinConfig

	override suspend fun setup() {
		scheduler = Scheduler()
		pastebinConfig = getPastebinConfig()

		scheduler?.schedule(taskDelay.minutes, repeat = true) {
			pastebinConfig = getPastebinConfig()
		}

		event<MessageCreateEvent> {
			// TODO
		}
	}

	override suspend fun unload() {
		scheduler?.shutdown()
	}

	private suspend fun getPastebinConfig(): PastebinConfig {
		val text = client.get(configUrl).bodyAsText()

		return yaml.decodeFromString(text)
	}
}
