/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors.quilt

import dev.kord.core.event.Event
import io.github.z4kn4fein.semver.Version
import org.quiltmc.community.cozy.modules.logs.api.QuiltMetaClient
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

public class QuiltLoaderVersionProcessor : LogProcessor() {
	override val identifier: String = "quilt-loader-version"
	override val order: Order = Order.Default

	private val metaClient = QuiltMetaClient()

	override suspend fun predicate(log: Log, event: Event): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		val currentVersion = log.getLoaderVersion(LoaderType.Quilt)!!.toSemver()
		val loaderVersions = metaClient.getLoaderVersions().sorted()

		val latestStable = loaderVersions
			.filter { it.isPreRelease.not() }
			.maxByOrNull { it }
			?: Version.min

		val latestPreRelease = loaderVersions
			.filter { it.isPreRelease }
			.maxByOrNull { it }
			?: Version.min

		val currentMax = if (currentVersion.isPreRelease) {
			maxOf(currentVersion, latestStable, latestPreRelease)
		} else {
			maxOf(currentVersion, latestStable)
		}

		if (currentMax > currentVersion) {
			log.hasProblems = true

			log.addMessage(
				"**Quilt Loader is out of date.** Latest version: `$currentMax`\n" +
						"**»** [Installation instructions](https://quiltmc.org/install/)"
			)
		}
	}
}
