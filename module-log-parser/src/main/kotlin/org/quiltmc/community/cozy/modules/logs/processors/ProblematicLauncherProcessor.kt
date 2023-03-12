/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors

import org.quiltmc.community.cozy.modules.logs.data.Launcher
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

public class ProblematicLauncherProcessor : LogProcessor() {
	override val identifier: String = "problematic-launcher"
	override val order: Order = Order.Earlier

	override suspend fun process(log: Log) {
		if (log.launcher?.name == Launcher.PolyMC) {
			log.abort(
				"**You seem to be using PolyMC.**\n\n" +

						"As of October 2022, **PolyMC has been taken over by transphobes, antisemites and so on.**" +
						"The project owner kicked out the existing development and community teams, and replaced them " +
						"with a group of infamous bigots - and the project is now **barely being maintained at " +
						"all.** As PolyMC downloads and runs software automatically (and this is done based " +
						"on information stored on a server that the project owner has control over), **it should " +
						"not be considered safe to use.**\n\n" +

						"Instead, you could move to [Prism Launcher](https://prismlauncher.org), which is a " +
						"continuation of PolyMC **by the developers that actually worked on it.** This team does " +
						"not support bigotry, and has no history of co-opting projects in order to spread it. " +
						"There are also many other launchers you could switch to.\n\n" +

						"For more information on what happened, feel free to check out the following links:\n\n" +

						"- [GamingOnLinux article](https://www.gamingonlinux.com/2022/10" +
						"/if-you-use-polymc-for-minecraft-you-should-switch-away-now/)\n" +

						"- [Thread by kingbdogz](https://twitter.com/kingbdogz/status/1582124211673628672)\n" +

						"- [Timeline by Protonull](https://blog.protonull.uk/2022-10-18-polymc-drama/)"
			)
		}
	}
}
