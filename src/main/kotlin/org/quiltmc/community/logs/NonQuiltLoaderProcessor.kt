/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.logs

import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

class NonQuiltLoaderProcessor : LogProcessor() {
	override val identifier: String = "non-quilt-loader"
	override val order: Order = Order.Early

	override suspend fun predicate(log: Log): Boolean =
		// TODO: Only run this in specific channels somehow?
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		log.addMessage(
			"You appear to be using a loader other than Quilt - please double-check that you have Quilt installed!"
		)
	}
}
