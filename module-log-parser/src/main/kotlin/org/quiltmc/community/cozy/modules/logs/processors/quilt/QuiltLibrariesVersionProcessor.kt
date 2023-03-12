/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors.quilt

import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

public class QuiltLibrariesVersionProcessor : LogProcessor() {
	override val identifier: String = "quilt-libraries-version"
	override val order: Order = Order.Default

	override suspend fun predicate(log: Log): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		TODO("Not yet implemented")
	}
}
