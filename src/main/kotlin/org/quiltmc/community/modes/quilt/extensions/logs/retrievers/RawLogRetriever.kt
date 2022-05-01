/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.retrievers

import dev.kord.core.entity.Message
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import mu.KotlinLogging

private const val SLUG: String = "([^/?#]*)"

private val strategyMap: Map<Regex, String> = mutableMapOf(
    "0x0.st/$SLUG".toRegex() to "https://0x0.st/$1",
    "bytebin.lucko.me/$SLUG".toRegex() to "https://bytebin.lucko.me/$1",
    "gist.github.com/$SLUG/$SLUG".toRegex() to "https://gist.githubusercontent.com/$1/$2/raw",
    "gist.githubusercontent.com/$SLUG/$SLUG/raw".toRegex() to "https://gist.githubusercontent.com/$1/$2/raw",
    "paste.ee/[pd]/$SLUG".toRegex() to "https://paste.ee/d/$1",

    "pastebin.com/$SLUG".toRegex() to "https://pastebin.com/dl/$1",
    "pastebin.com/(raw|dl|clone|embed|print)/$SLUG".toRegex() to "https://pastebin.com/dl/$2",
)

class RawLogRetriever : BaseLogRetriever {
    val client = HttpClient {}
    val logger = KotlinLogging.logger {}

    override suspend fun getLogContent(message: Message): List<String> {
        val results: MutableList<String> = mutableListOf()

        strategyMap.forEach { (key, value) ->
            val matches = key.findAll(message.content)

            matches.forEach { match ->
                var url = value

                match.groups.forEachIndexed { index, matchGroup ->
                    url = url.replace("$$index", matchGroup!!.value)
                }

                @Suppress("TooGenericExceptionCaught")
                try {
                    val response: String = client.get(url).bodyAsText()

                    results.add(response)
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to retrieve logs from URL: $url" }
                }
            }
        }

        return results
    }
}
