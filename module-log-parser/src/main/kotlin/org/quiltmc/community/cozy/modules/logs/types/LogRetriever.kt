/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.types

import dev.kord.core.event.Event
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.koin.KordExKoinComponent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.koin.core.component.inject
import org.quiltmc.community.cozy.modules.logs.LogParserExtension
import java.net.URL

@Suppress("FunctionNaming")
public abstract class LogRetriever : BaseLogHandler, KordExKoinComponent {
	private val bot: ExtensibleBot by inject()
	protected val extension: LogParserExtension get() = bot.findExtension()!!

	protected val client: HttpClient = HttpClient(CIO) {
		install(ContentNegotiation) {
			json(
				kotlinx.serialization.json.Json { ignoreUnknownKeys = true },
				ContentType.Any
			)
		}
	}

	public abstract suspend fun process(url: URL): Set<String>

	public open suspend fun setup() {}

	protected open suspend fun predicate(url: URL, event: Event): Boolean =
		true

	/** @suppress Internal function; use for intermediary types only. **/
	public open suspend fun _predicate(url: URL, event: Event): Boolean =
		predicate(url, event)
}
