/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.parsers

private const val IP_REGEX =
    "[a-zA-Z]+\\[/[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+:[0-9]+] logged in with entity id"

class PlayerIPParser : BaseLogParser {
    override suspend fun getMessages(logContent: String): List<String> {
        val messages: MutableList<String> = mutableListOf()

        if (logContent.contains(IP_REGEX)) {
            @Suppress("MaximumLineLength")
            messages.add(
                "You appear to have shared player IP addresses. " +
                        "You may want to delete that and re-upload it without them."
            )
        }

        return messages
    }
}
