package org.quiltmc.community.cozy.modules.logs.parsers.fabric

import org.quiltmc.community.cozy.modules.logs.Version
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Mod
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogParser

private val OPENING_LINE = "Loading \\d+ mods:\n".toRegex(RegexOption.IGNORE_CASE)
private val CLOSE = "\n[^\\s-]+".toRegex(RegexOption.IGNORE_CASE)

public class FabricModsParser : LogParser() {
	override val identifier: String = "mods-fabric"
	override val order: Order = Order.Default

	override suspend fun predicate(log: Log): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		val start = log.content.split(OPENING_LINE, 2).last()
		val list = start.split(CLOSE, 2).first().trim()

		list.split("\n")
			.map { it.trim().trimStart('-', ' ') }
			.forEach {
				val split = it.split(" ", limit = 2)

				log.addMod(
					Mod(
						split.first(),
						Version(split.last())
					)
				)
			}

		log.environment.javaVersion = log.getMod("java")!!.version.string
	}
}
