package org.quiltmc.community.cozy.modules.logs.parsers.launchers

import org.quiltmc.community.cozy.modules.logs.data.Launcher
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogParser

private val INFO_LINE_REGEX = "Launching Minecraft with the following arguments \\([^)]+\\): \\[([^]]+)]\n"
	.toRegex(RegexOption.IGNORE_CASE)

private val OS_REGEX = "-Dos.name=(.+?), -".toRegex(RegexOption.IGNORE_CASE)

public class ATLauncherParser : LogParser() {
	override val identifier: String = "launcher-atlauncher"
	override val order: Order = Order.Early

	override suspend fun predicate(log: Log): Boolean =
		log.launcher?.name == Launcher.ATLauncher

	override suspend fun process(log: Log) {
		val infoString = INFO_LINE_REGEX.findAll(log.content).lastOrNull()?.groupValues?.getOrNull(1)
			?: return

		log.environment.jvmArgs = infoString.substringBefore(", -Djava.library.path")

		val osString = OS_REGEX.find(infoString)?.groupValues?.getOrNull(1)

		if (osString != null) {
			log.environment.os = osString
		}
	}
}
