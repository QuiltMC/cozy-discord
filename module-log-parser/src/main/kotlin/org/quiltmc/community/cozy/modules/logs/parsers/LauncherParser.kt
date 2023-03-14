/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.parsers

import org.quiltmc.community.cozy.modules.logs.data.Launcher
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogParser

private val PATTERNS = mapOf(
	Launcher.ATLauncher to "minecraft.launcher.brand=ATLauncher".toRegex(RegexOption.IGNORE_CASE),
	Launcher.MultiMC to "MultiMC version: (\\S+)".toRegex(RegexOption.IGNORE_CASE),
	Launcher.Prism to "Prism Launcher version: (\\S+)".toRegex(RegexOption.IGNORE_CASE),
	Launcher.PolyMC to "PolyMC version: (\\S+)".toRegex(RegexOption.IGNORE_CASE),
	Launcher.Technic to "api\\.technicpack\\.net resolves to".toRegex(RegexOption.IGNORE_CASE),

	Launcher.TLauncher to "Starting TLauncher ([^\n]+)".toRegex(RegexOption.IGNORE_CASE),
	Launcher.TLauncher to "tlauncher\\.org".toRegex(RegexOption.IGNORE_CASE),
)

public class LauncherParser : LogParser() {
	override val identifier: String = "launcher"
	override val order: Order = Order.Earlier

	// You tried, Detekt.
	@Suppress("UnconditionalJumpStatementInLoop")
	override suspend fun process(log: Log) {
		val (launcher, match) = PATTERNS.entries
			.map { (launcher, pattern) -> Pair(launcher, pattern.find(log.content)) }
			.firstOrNull() { (_, match) -> match != null }
			?: return

		log.launcher = Launcher(launcher, match!!.groups[1]?.value)

		if (launcher == Launcher.Prism) {
			log.addMessage(
				"**It looks like you're using Prism.** Please note that Prism [currently breaks uploaded logs]" +
						"(https://github.com/PrismLauncher/PrismLauncher/issues/930), and the information " +
						"provided here may be incomplete. Until this is fixed, please consider manually " +
						"uploading your `latest.log` file to the channel instead."
			)
		}
	}
}
