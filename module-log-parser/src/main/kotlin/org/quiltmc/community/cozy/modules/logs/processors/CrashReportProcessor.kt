/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors

import dev.kord.core.event.Event
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

private val CRASH_REPORT_REGEX = "Crashed! The full crash report has been saved to (\\S+)"
	.toRegex(RegexOption.IGNORE_CASE)

public class CrashReportProcessor : LogProcessor() {
	override val identifier: String = "crash-reports"
	override val order: Order = Order.Default

	override suspend fun predicate(log: Log, event: Event): Boolean =
		CRASH_REPORT_REGEX.find(log.content) != null

	override suspend fun process(log: Log) {
		val match = CRASH_REPORT_REGEX.find(log.content) ?: return
		log.addMessage("Please also provide the crash report at `${match.groups[1]!!.value}`")
	}
}
