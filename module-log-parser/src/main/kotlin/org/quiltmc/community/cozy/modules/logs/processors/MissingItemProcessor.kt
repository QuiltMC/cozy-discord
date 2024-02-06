/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors

import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor

// ClassNotFoundException: package.name.ClassName
private val CLASS_REGEX =
	"""java\.lang\.ClassNotFoundException: ((?:[^\n.]+[\.\/])*)([^\n.()]+)"""
	.toRegex(RegexOption.IGNORE_CASE)

// NoSuchMethodError: 'ReturnType package.name.ClassName.methodName(ParamType, ParamType, ParamType)'
private val METHOD_REGEX =
	"""java\.lang\.NoSuchMethodError: '([^\n]+) ((?:[^\n.]+[\.\/])*)([^\n.()]+)\.([^\n.]+)\.([^\n(]+)\(([^)]*)\)"""
	.toRegex(RegexOption.IGNORE_CASE)

// This is the new format for NoSuchFieldError (the old one has no useful information)
// NoSuchFieldError: Class package.name.ClassName does not have member field 'Type fieldName'
private val FIELD_REGEX =
	"""java\.lang\.NoSuchFieldError: Class ((?:[^\n.]+[\.\/])*)([^\n.()]+) does not have member field '([^\n]+)'"""
	.toRegex(RegexOption.IGNORE_CASE)

public class MissingItemProcessor : LogProcessor() {
	override val identifier: String = "missing-jvm-elements"
	override val order: Order = Order.Default

	@Suppress("MagicNumber")
	override suspend fun process(log: Log) {
		val classMatches = CLASS_REGEX.findAll(log.content).toList()
		val methodMatches = METHOD_REGEX.findAll(log.content).toList()
		val fieldMatches = FIELD_REGEX.findAll(log.content).toList()

		val packages = mutableSetOf<String>()
		val topLevelClasses = mutableSetOf<String>()

		for (match in classMatches) {
			val pkg = match.groupValues[1]
			if (pkg.isNotBlank()) {
				packages.add(pkg.replace('/', '.'))
			} else {
				topLevelClasses.add(match.groupValues[2])
			}
		}

		for (match in methodMatches) {
			val pkg = match.groupValues[2]
			if (pkg.isNotBlank()) {
				packages.add(pkg.replace('/', '.'))
			} else {
				topLevelClasses.add(match.groupValues[3])
			}
		}

		for (match in fieldMatches) {
			val pkg = match.groupValues[1]
			if (pkg.isNotBlank()) {
				packages.add(pkg.replace('/', '.'))
			} else {
				topLevelClasses.add(match.groupValues[2])
			}
		}

		if (packages.isNotEmpty()) {
			log.addMessage(
				buildString {
					append(
						"**Some packages did not contain all required classes, fields, or methods. " +
						"This is usually caused by a missing dependency.**\n"
					)
					for (pkg in packages) {
						append("- `$pkg`\n")
					}
				}
			)
		}

		if (topLevelClasses.isNotEmpty()) {
			log.addMessage(
				buildString {
					append(
						"**Some top-level classes were not found or were missing fields or methods. " +
						"This is usually caused by a missing dependency.**\n"
					)
					for (cls in topLevelClasses) {
						append("- `$cls`\n")
					}
				}
			)
		}
	}
}
