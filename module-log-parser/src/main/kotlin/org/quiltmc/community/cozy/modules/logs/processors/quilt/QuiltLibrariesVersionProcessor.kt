/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors.quilt

import dev.kord.core.event.Event
import io.github.z4kn4fein.semver.Version
import org.quiltmc.community.cozy.modules.logs.api.ModrinthClient
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

public class QuiltLibrariesVersionProcessor : LogProcessor() {
	override val identifier: String = "quilt-libraries-version"
	override val order: Order = Order.Default

	private val modrinthClient = ModrinthClient()

	override suspend fun predicate(log: Log, event: Event): Boolean =
		(log.minecraftVersion != null || log.getMod("minecraft") != null) &&
				log.getLoaderVersion(LoaderType.Quilt) != null &&
				log.getMod("quilted_fabric_api") != null

	override suspend fun process(log: Log) {
		val mcVersion = log.minecraftVersion?.string
			?: log.getMod("minecraft")?.version?.string
			?: return

		val modVersion = log.getMod("quilted_fabric_api")!!.version.toSemver()

		val currentVersions = modrinthClient.getProjectVersions("qsl")
			?.filter {
				it.gameVersions.any { v -> v.equals(mcVersion, true) }
			}
			?.map { Version.parse(it.versionNumber) }
			?: return

		val latestVersion = maxOf(modVersion, currentVersions.max())

		if (latestVersion > modVersion) {
			log.hasProblems = true

			log.addMessage(
				"**QSL/QFAPI is out of date.** Latest version: `$latestVersion`\n" +
						"**»** [Modrinth](https://modrinth.com/mod/qsl/)\n" +
						"**»** [CurseForge](https://www.curseforge.com/minecraft/mc-mods/qsl)"
			)
		}
	}
}
