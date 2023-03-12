/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.retrievers

import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.data.PastebinConfig
import org.quiltmc.community.cozy.modules.logs.data.ScrapeType
import org.quiltmc.community.cozy.modules.logs.types.LogRetriever
import java.net.URL

public class PastebinLogRetriever : LogRetriever() {
	override val identifier: String = "pastebin"
	override val order: Order = Order.Early

	private val config: PastebinConfig get() = extension.pastebinConfig

	override suspend fun process(url: URL): Set<String> {
		val string = url.toString()

		for (pattern in config.raw) {
			if (pattern.find(string) != null) {
				return setOf(client.get(url).bodyAsText())
			}
		}

		for (transform in config.urlTransform) {
			val match = transform.match.find(string)
				?: continue

			var target = transform.output

			if (transform.split != null) {
				val parts = match
					.groups[transform.split.group]!!
					.value
					.split(transform.split.string)

				val result: MutableSet<String> = mutableSetOf()

				parts.forEach {
					target = transform.output
						.replace("$${transform.split.group}", it)

					match.groups.filterNotNull().forEachIndexed { index, group ->
						target = target.replace("$$index", group.value)
					}

					result.add(client.get(target).bodyAsText())
				}

				return result
			}

			match.groups.filterNotNull().forEachIndexed { index, group ->
				target = target.replace("$$index", group.value)
			}

			return setOf(client.get(target).bodyAsText())
		}

		for (scrape in config.scrape) {
			if (scrape.match.find(string) == null) {
				continue
			}

			val soup = Jsoup.connect(string).get()
			val elements = soup.select(scrape.selector)

			when (scrape.type) {
				ScrapeType.FirstElement ->
					return setOf(
						elements.firstOrNull()?.text()
							?: continue
					)

				ScrapeType.Hrefs -> {
					val results: MutableSet<String> = mutableSetOf()

					val links = elements.mapNotNull {
						if (it.hasAttr("href")) {
							it.attr("href")
						} else {
							null
						}
					}

					if (links.isEmpty()) {
						continue
					}

					links.forEach {
						results.add(client.get(it).bodyAsText())
					}

					return results
				}
			}
		}

		return setOf()
	}
}
