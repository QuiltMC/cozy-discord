package org.quiltmc.community.cozy.modules.logs.processors.quilt

import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

public class QuiltLoaderVersionProcessor : LogProcessor() {
	override val identifier: String = "fabric-loader-version"
	override val order: Order = Order.Default

	override suspend fun predicate(log: Log): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		TODO("Not yet implemented")
	}
}
