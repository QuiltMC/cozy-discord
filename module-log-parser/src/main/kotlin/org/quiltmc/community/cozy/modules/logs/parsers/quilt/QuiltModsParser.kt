/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.parsers.quilt

import dev.kord.core.event.Event
import dev.kordex.parser.Cursor
import org.quiltmc.community.cozy.modules.logs.Version
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Mod
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogParser

private val OPENING_LINES = arrayOf(
	"Loading \\d+ mods:\\n".toRegex(RegexOption.IGNORE_CASE),
	"-- Mods --\\n".toRegex(RegexOption.IGNORE_CASE),
	"-- Mod Table --\\n".toRegex(RegexOption.IGNORE_CASE),
	"\\tQuilt Mods: \\n".toRegex(RegexOption.IGNORE_CASE),
)

private val CLOSE = "\\n[^|\\n]*\\n|$".toRegex(RegexOption.IGNORE_CASE)

public class QuiltModsParser : LogParser() {
	override val identifier: String = "mods-quilt"
	override val order: Order = Order.Default

	override suspend fun predicate(log: Log, event: Event): Boolean = log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		val openingLine = OPENING_LINES.first { it in log.content }

		val start = log.content.split(openingLine, 2).last()
		val table = start.split(CLOSE, 2).first().trim()

		val lines = table
			.split("\n")
			.map { it.trim().trim('|') }  // Don't strip spaces here, but do remove border pipes
			.toMutableList()

		// The first line is the headers
		val headerNames = lines
			.removeFirst()
			.split("|")
			.map { it.trim().lowercase() }

		// The second line gives us the separators, which we can use to figure out how wide each column is
		val separatorLengths = lines
			.removeFirst()
			.split("|")
			.map { it.count() }

		// The last line is also always just separators
		lines.removeLast()

		lines.forEach { line ->
			// We can't split as individual entries may contain pipe chars, so we need to do this by column width.
			val mod: MutableMap<String, String> = mutableMapOf()
			val cursor = Cursor(line)  // KordEx string-parsing cursor is a good match for this

			headerNames.forEachIndexed { index, header ->
				val width = separatorLengths[index]

				mod[header] = cursor.consumeNumber(width).trim()

				cursor.nextOrNull()  // Advance the cursor
			}

			log.addMod(
				Mod(
					mod["id"]!!,
					Version(mod["version"]!!),
					mod["file(s)"],
					mod["file hash (sha-1)"],
					mod["type"]
				)
			)
		}

		log.environment.javaVersion = log.getMod("java")!!.version.string
	}
}
