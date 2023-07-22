/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors

import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

private val MIXIN_ERROR_REGEXES = arrayOf(
	// Failed during APPLY phase
	(
		"Caused by: org.spongepowered.asm.mixin.throwables.MixinApplyError: Mixin \\[\\S+ from mod ([^\\]]+)\\] from " +
			"phase \\[\\w+\\] in config \\[[^\\]]+\\] FAILED during APPLY"
		).toRegex(RegexOption.IGNORE_CASE),

	// Critical injection failure
	"mod (\\S+) failed injection check, \\(\\d/\\d\\) succeeded. Scanned \\d target\\(s\\)\\."
		.toRegex(RegexOption.IGNORE_CASE)
)

public class MixinErrorProcessor : LogProcessor() {
	override val identifier: String = "mixin-error"
	override val order: Order = Order.Earlier

	override suspend fun process(log: Log) {
		val matches = MIXIN_ERROR_REGEXES
			.map { it.findAll(log.content).toList() }
			.flatten()

		if (matches.isEmpty()) {
			return
		}

		val mods = matches
			.map { it.groupValues[1] }
			.toSet()
			.joinToString(", ") { "`$it`" }

		log.addMessage("**Found ${matches.size} mixin failures in the following mods:** $mods")
		log.hasProblems = true
	}
}
