/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.logs

import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

private val BAD_MODS = mapOf(
	"fabric_hider" to "Fabric Hider",
)

private const val SITE_LINK =
	"https://quiltmc.org/en/community/rules/#5-keep-all-projects-legal-legitimate--appropriate"

class RuleBreakingModProcessor : LogProcessor() {
	override val identifier: String = "rule-breaking-mod"
	override val order: Order = Order.Early

	override suspend fun process(log: Log) {
		val mods = log.getMods().filter { it.key in BAD_MODS }

		if (mods.isEmpty()) {
			return
		}

		log.abort(
			buildString {
				append("You appear to have the following rule-breaking mods installed: ")

				appendLine(
					mods
						.map { BAD_MODS[it.key] }
						.toSet()
						.sortedBy { it }
						.joinToString { "**$it**" }
				)

				appendLine()

				append(
					"For more information, please see [rule 5 on the site]($SITE_LINK). Please note that we cannot " +
							"provide you with support while you're using mods that break our rules."
				)
			}
		)
	}
}
