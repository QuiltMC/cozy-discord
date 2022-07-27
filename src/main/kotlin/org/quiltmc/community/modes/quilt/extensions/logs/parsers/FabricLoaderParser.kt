/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.parsers

private val LOADER_REGEX = "\\[fabric-loader-[\\d.]+\\.jar:\\?]".toRegex()

class FabricLoaderParser : BaseLogParser {
	override suspend fun getMessages(logContent: String): List<String> {
		val messages: MutableList<String> = mutableListOf()

		if (logContent.contains(LOADER_REGEX)) {
			messages.add(
				"You appear to be using Fabric instead of Quilt - please double-check that you have Quilt installed"
			)
		}

		return messages
	}
}
