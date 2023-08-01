/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors

import dev.kord.core.event.Event
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

private val ERROR_REGEX = (
	"has been compiled by a more recent version of the Java Runtime " +
		"\\(class file version ([\\d\\.]+)\\), this version of the Java Runtime only recognizes class file versions " +
		"up to ([\\d\\.]+)"
	).toRegex(RegexOption.IGNORE_CASE)

// I'm not sure why it thinks these aren't constants, but okay...
@Suppress("MagicNumber")
private val VERSION_MAP = mutableMapOf(
	45 to "1.1",
	46 to "1.2",
	47 to "1.3",
	48 to "1.4",
	49 to "5",
	50 to "6",
	51 to "7",
	52 to "8",
	53 to "9",
	54 to "10",
	55 to "11",
	56 to "12",
	57 to "13",
	58 to "14",
	59 to "15",
	60 to "16",
	61 to "17",
	62 to "18",
	63 to "19",
	64 to "20",
	65 to "21",
)

public class JavaClassFileVersionProcessor : LogProcessor() {
	override val identifier: String = "java-class-file-version"
	override val order: Order = Order.Earlier

	override suspend fun predicate(log: Log, event: Event): Boolean =
		log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		val matches = ERROR_REGEX.findAll(log.content).toList()

		if (matches.isEmpty()) {
			return
		}

		// Required -> Current
		val versions = matches.map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
		val currentClass = versions.first().second

		var requiredClass: Int = 0

		versions.forEach { requiredClass = maxOf(requiredClass, it.first) }

		val current = VERSION_MAP[currentClass]
		val required = VERSION_MAP[requiredClass]

		log.addMessage(
			buildString {
				append("**It looks like you need to update Java.** ")

				append(
					if (current != null) {
						"You have Java `$current` installed, "
					} else {
						"You have a version of Java that supports up to class version `$currentClass`, "
					}
				)

				append(
					if (required != null) {
						"but you need Java `$required` or later."
					} else {
						"but you need a version of java that supports class version `$requiredClass` or later."
					}
				)
			}
		)

		log.hasProblems = true
	}
}
