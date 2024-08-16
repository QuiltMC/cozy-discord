/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors

import dev.kord.core.event.Event
import dev.kordex.core.DISCORD_YELLOW
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

private val UNKNOWN_MOD_REGEX = "Unknown file in mods folder: <mods>[\\\\/]([^\n]+)\n".toRegex(RegexOption.IGNORE_CASE)

public class UnknownModProcessor : LogProcessor() {
	override val identifier: String = "unknown-mod"
	override val order: Order = Order.Earlier

	override suspend fun predicate(log: Log, event: Event): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		val matches = UNKNOWN_MOD_REGEX.findAll(log.content).toList()

		if (matches.isEmpty()) {
			return
		}

		val mods = matches.map { it.groupValues[1] }

		log.embed {
			title = "Unknown Mods"
			color = DISCORD_YELLOW

			description = "**The following unknown mods were found.** \n\n" +
				"This usually means you've installed mods that Quilt doesn't know how to load. We recommend using " +
				"separate instance folders for different mod loaders, to avoid confusion and unintended " +
				"behaviour.\n\n" +

				mods.joinToString(", ") { "`$it`" }
		}

		log.hasProblems = true
	}
}
