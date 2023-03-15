/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors.quilt

import dev.kord.core.event.Event
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor
import kotlin.time.Duration.Companion.minutes

private const val THREAD_LINK = "https://forum.quiltmc.org/t/mod-incompatibility-megathread/261"
private const val THREAD_JSON = "$THREAD_LINK.json"

private val CHECK_DELAY = 15.minutes

public class IncompatibleModProcessor : LogProcessor() {
	override val identifier: String = "quilt-incompatible-mod"
	override val order: Order = Order.Default

	private val refreshLock = Mutex()

	private val json = Json {
		ignoreUnknownKeys = true
	}

	private var lastCheck: Instant? = null
	private var incompatibleMods: List<IncompatibleMod> = listOf()

	override suspend fun predicate(log: Log, event: Event): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		refreshIncompatibleMods()

		val mods = incompatibleMods.filter {
			it.ids.any { id ->
				id in log.getMods().keys
			}
		}

		if (mods.isEmpty()) {
			return
		}

		var typedMods: Map<Incompatibility, MutableList<IncompatibleMod>> = sortedMapOf(
			{ left, right -> left.order.compareTo(right.order) },

			Incompatibility.GAME to mutableListOf(),
			Incompatibility.OTHERS to mutableListOf(),
			Incompatibility.SELF to mutableListOf(),
		)

		mods.forEach { mod ->
			typedMods[mod.type]!!.add(mod)
		}

		typedMods = typedMods.filterValues { it.isNotEmpty() }

		log.hasProblems = true

		log.addMessage(
			buildString {
				appendLine("**Potentially incompatible mods installed:**")
				appendLine()

				typedMods.forEach { (type, mods) ->
						append("**» ${type.readable}:** ")
						appendLine(mods.joinToString { it.name })
					}

				appendLine()
				append(
					"**Note:** The list of incompatible mods may not be perfectly up-to-date. For more " +
							"information, please see [the Quilt mod incompatibility mega-thread]($THREAD_LINK)."
				)
			}
		)
	}

	private suspend fun refreshIncompatibleMods() {
		refreshLock.withLock {
			val now = Clock.System.now()

			if (lastCheck != null) {
				val checkThreshold = now - CHECK_DELAY

				if (lastCheck!! > checkThreshold) {
					return
				}
			}

			val postText = client.get(THREAD_JSON)
				.body<DiscoursePosts>()
				.postStream
				.posts
				.first()
				.cooked

			val cozyJson = postText
				.substringAfter("// START COZY JSON")
				.substringBefore("// END COZY JSON")

			incompatibleMods = json.decodeFromString(cozyJson)
			lastCheck = now
		}
	}

	@Serializable
	public enum class Incompatibility(public val order: Int, public val readable: String) {
		GAME(0, "Breaks the game"),
		OTHERS(1, "Breaks other mods"),
		SELF(2, "Non-fatal errors or broken features")
	}

	@Serializable
	public data class IncompatibleMod(
		public val ids: List<String>,
		public val name: String,
		public val type: Incompatibility,
	)

	@Serializable
	public data class DiscoursePosts(
		@SerialName("post_stream")
		public val postStream: PostStream
	)

	@Serializable
	public data class PostStream(
		public val posts: List<CookedPost>
	)

	@Serializable
	public data class CookedPost(
		public val cooked: String
	)
}
