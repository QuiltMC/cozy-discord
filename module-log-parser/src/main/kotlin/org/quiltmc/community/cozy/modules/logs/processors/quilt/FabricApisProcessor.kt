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

public class FabricApisProcessor : LogProcessor() {
	override val identifier: String = "quilt-fabric-apis"
	override val order: Order = Order.Default

	override suspend fun predicate(log: Log, event: Event): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		val fabricApi = log.getMod("fabric")
		val fabricLanguageKotlin = log.getMod("fabric-language-kotlin")
		val quiltStandardLibraries = log.getMod("qsl")

		if (fabricApi != null) {
			log.hasProblems = true

			log.addMessage(
				"Fabric API is present at `${fabricApi.path}`, and should be replaced by [QSL](https://modrinth.com/mod/qsl)"
			)
		}

		if (fabricLanguageKotlin != null && quiltStandardLibraries != null) {
			log.hasProblems = true

			log.addMessage(
				"Fabric Language Kotlin is present at `${fabricLanguageKotlin.path}`, " +
					"and should be replaced by [QKL](https://modrinth.com/mod/qkl)"
			)
		}
	}
}
