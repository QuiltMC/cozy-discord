/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors.quilt

import dev.kord.core.event.Event
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

public class FabricImplProcessor : LogProcessor() {
	override val identifier: String = "quilt-fabric-impl"
	override val order: Order = Order.Default

	override suspend fun predicate(log: Log, event: Event): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		var classNotFoundLine: Int? = null
		var suspectedMod: String? = null
		var suspectedPackage: String? = null

		for ((index, line) in log.content.lines().mapIndexed { index, s -> index to s }) {
			if (line.startsWith("java.lang.RuntimeException: Could not execute entrypoint stage")) {
				classNotFoundLine = index

				suspectedMod = line
					.split("' due to errors, provided by '")
					.lastOrNull()
					?.split("'")
					?.firstOrNull()
					?.trim()

				continue
			}

			if (classNotFoundLine == null || suspectedMod == null) {
				continue
			}

			if (line.startsWith("Caused by: java.lang.ClassNotFoundException:")) {
				suspectedPackage = line.split("ClassNotFoundException:").lastOrNull()?.trim()

				if (suspectedPackage != null) {
					break
				}
			}
		}

		if (
			suspectedMod != null &&
			suspectedPackage != null &&
			".fabricmc." in suspectedPackage && (".impl." in suspectedPackage || ".mixin." in suspectedPackage)
		) {
			log.hasProblems = true

			log.addMessage(
				"Mod `$suspectedMod` may be using Fabric internals:\n`$suspectedPackage`"
			)
		}
	}
}
