/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")

package org.quiltmc.community.cozy.modules.logs.parsers

import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogParser
import kotlin.math.roundToInt

// Note: I see this in some crash logs, and I don't really understand where it's coming from. However, we parsin'
// b/c this is still useful info.  -- gdude

private val CPU_REGEX = "\tProcessor Name: ([^\n]+)\n".toRegex(RegexOption.IGNORE_CASE)
private val CPU_CORES_REGEX = "\tNumber of physical CPUs: (\\d+)\n".toRegex(RegexOption.IGNORE_CASE)
private val CPU_THREADS_REGEX = "\tNumber of logical CPUs: (\\d+)\n".toRegex(RegexOption.IGNORE_CASE)

private val GPU_REGEX = "\tBackend API: ([^\n]+)\n".toRegex(RegexOption.IGNORE_CASE)

private val GAME_MEMORY_REGEX =
	"\tMemory: \\d+ bytes \\((\\d+) MiB\\) / \\d+ bytes \\((\\d+) MiB\\) up to \\d+ bytes \\((\\d+) MiB\\)\n\n"
		.toRegex(RegexOption.IGNORE_CASE)

private val SHADERPACK_REGEX = "\tLoaded Shaderpack: ([^\n]+)\n".toRegex(RegexOption.IGNORE_CASE)
private val SYSTEM_MEMORY_REGEX = "\tMemory slot #\\d+ capacity \\(MB\\): ([\\d.]+)".toRegex(RegexOption.IGNORE_CASE)

public class EnvironmentParser : LogParser() {
	override val identifier: String = "environment"
	override val order: Order = Order.Default

	override suspend fun process(log: Log) {
		val cpuMatch = CPU_REGEX.find(log.content)
		val gpuMatch = GPU_REGEX.find(log.content)
		val shaderpackMatch = SHADERPACK_REGEX.find(log.content)

		val gameMemoryMatch = GAME_MEMORY_REGEX.find(log.content)
		val systemMemoryMatch = SYSTEM_MEMORY_REGEX.findAll(log.content).toList()

		if (cpuMatch != null) {
			log.environment.cpu = buildString {
				append(cpuMatch.groupValues[1])

				val cores = CPU_CORES_REGEX.find(log.content)
				val threads = CPU_THREADS_REGEX.find(log.content)

				if (cores != null || threads != null) {
					append(" (")

					if (cores != null) {
						append("${cores.groupValues[1]} cores")

						if (threads != null) {
							append(", ")
						}
					}

					if (threads != null) {
						append("${threads.groupValues[1]} threads")
					}

					append(")")
				}
			}
		}

		if (gpuMatch != null) {
			log.environment.gpu = gpuMatch.groupValues[1]
		}

		if (shaderpackMatch != null) {
			log.environment.shaderpack = shaderpackMatch.groupValues[1]
		}

		if (gameMemoryMatch != null) {
			val current = gameMemoryMatch.groupValues[1]
			val allocated = gameMemoryMatch.groupValues[2]
			val max = gameMemoryMatch.groupValues[3]

			log.environment.gameMemory = "$current MiB / $allocated MiB ($max MiB max)"
		}

		if (systemMemoryMatch.isNotEmpty()) {
			var total = 0.0

			systemMemoryMatch.forEach {
				total += it.groupValues[1].toDouble()
			}

			log.environment.systemMemory = "${total.roundToInt()} MiB"
		}
	}
}
