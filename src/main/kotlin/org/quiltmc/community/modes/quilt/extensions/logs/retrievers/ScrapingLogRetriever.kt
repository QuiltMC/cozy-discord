/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.retrievers

import dev.kord.core.entity.Message
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private const val SLUG: String = "([^/?#]*)"

class ScrapingLogRetriever : BaseLogRetriever {
	val client = HttpClient {}
	val logger = KotlinLogging.logger {}

	private val strategyMap: Map<Regex, SiteType> = mutableMapOf(
		"https://paste.atlauncher.com/view/$SLUG".toRegex() to SiteType.ATLAUNCHER,
		"https://paste.gg/p/$SLUG/$SLUG".toRegex() to SiteType.PASTEGG,
	)

	override suspend fun getLogContent(message: Message): List<String> {
		val results: MutableList<String> = mutableListOf()

		strategyMap.forEach { (key, value) ->
			val matches = key.findAll(message.content)

			matches.forEach { match ->
				val url = match.value

				@Suppress("TooGenericExceptionCaught")
				try {
					val soup = Jsoup.connect(url).get()
					val result = retrieve(soup, value)

					results.addAll(result)
				} catch (e: Exception) {
					logger.warn(e) { "Failed to scrape logs from URL: $url" }
				}
			}
		}

		return results
	}

	private suspend fun retrieve(soup: Document, type: SiteType) = when (type) {
		SiteType.ATLAUNCHER -> retrieveAtLauncher(soup)
		SiteType.PASTEGG -> retrievePasteGG(soup)
	}

	private fun retrieveAtLauncher(soup: Document): List<String> {
		val results: MutableList<String> = mutableListOf()
		val result = soup.body().getElementsByTag("code").firstOrNull()?.text()

		if (result != null) {
			results.add(result)
		}

		return results
	}

	private suspend fun retrievePasteGG(soup: Document): List<String> {
		val results: MutableList<String> = mutableListOf()

		soup.body().getElementsByClass("box-title").mapNotNull { element ->
			element.getElementsByTag("a").firstOrNull()?.attr("href")
		}.forEach {
			val url = if ("://" in it) {
				it
			} else {
				"https://paste.gg$it"
			}

			@Suppress("TooGenericExceptionCaught")
			try {
				val paste = client.get(url).bodyAsText()

				results.add(paste)
			} catch (e: Exception) {
				logger.warn(e) { "[paste.gg] Failed to retrieve logs from URL: $url" }
			}
		}

		return results
	}

	private enum class SiteType {
		ATLAUNCHER,
		PASTEGG,
	}
}
