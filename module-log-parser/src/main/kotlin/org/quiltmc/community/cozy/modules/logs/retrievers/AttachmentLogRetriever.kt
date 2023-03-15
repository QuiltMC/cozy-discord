/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.retrievers

import dev.kord.core.event.Event
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.core.*
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.endsWithExtensions
import org.quiltmc.community.cozy.modules.logs.types.LogRetriever
import java.net.URL

private val DOMAINS: Array<String> = arrayOf(
	"cdn.discord.com",
	"cdn.discordapp.com",
	"media.discord.com",
	"media.discordapp.com",
)

private val EXTENSIONS: Array<String> = arrayOf(
	"log",
	"txt"
)

public class AttachmentLogRetriever : LogRetriever() {
	override val identifier: String = "message-attachment"
	override val order: Order = Order.Earlier

	@Suppress("SpreadOperator")
	override suspend fun predicate(url: URL, event: Event): Boolean =
		url.host in DOMAINS && (
				url.path.endsWithExtensions(*EXTENSIONS) ||
						'.' !in url.path.substringAfterLast('/')
				)

	override suspend fun process(url: URL): Set<String> =
		setOf(client.get(url).bodyAsChannel().readRemaining().readBytes().decodeToString())
}
