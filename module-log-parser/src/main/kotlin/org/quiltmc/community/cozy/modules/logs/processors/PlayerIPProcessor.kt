/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors

import dev.kord.core.event.Event
import dev.kordex.core.utils.runSuspended
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor
import java.net.InetAddress
import java.net.UnknownHostException

private val IPV4_REGEX = (
		"\\[\\/((?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)){3}):" +
				"[0-9]+] logged in with entity id"
		).toRegex()

private val IPV6_REGEX =
	"\\[/\\[((?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})]:[0-9]+] logged in with entity id"
		.toRegex()

public class PlayerIPProcessor : LogProcessor() {
	override val identifier: String = "player-ip"
	override val order: Order = Order.Early

	override suspend fun predicate(log: Log, event: Event): Boolean =
		!log.fromCommand

	override suspend fun process(log: Log) {
		val addresses = runSuspended { // InetAddress doesn't work with suspending functions
			(IPV4_REGEX.findAll(log.content) + IPV6_REGEX.findAll(log.content)).mapNotNull {
				try {
					InetAddress.getByName(it.groups[1]!!.value)
				} catch (e: UnknownHostException) {
					null
				}
			}
		}.filter { !it.isLoopbackAddress }.toList()

		if (addresses.isNotEmpty()) {
			log.addMessage(
				"This log appears to contain players' IP addresses. Please consider deleting your log, and posting " +
						"it again with the IP addresses removed."
			)
		}
	}
}
