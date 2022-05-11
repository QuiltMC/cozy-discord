/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.parsers

import java.net.InetAddress
import java.net.UnknownHostException

private val IPV4_REGEX =
    "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)){3}".toRegex()
private val IPV6_REGEX =
    "(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}".toRegex()

class PlayerIPParser : BaseLogParser {
    override suspend fun getMessages(logContent: String): List<String> {
        val content = if (logContent.contains("Setting user:")) {
            logContent.substringAfter("Setting user:")
        } else {
            logContent
        }

        val messages: MutableList<String> = mutableListOf()

        val ips = (IPV4_REGEX.findAll(content) + IPV6_REGEX.findAll(content)).mapNotNull {
            return@mapNotNull try {
                InetAddress.getByName(it.value)
            } catch (e: UnknownHostException) {
                null
            }
        }.filter { !it.isLoopbackAddress }.toList()

        if (ips.isNotEmpty()) {
            messages.add(
                "This log appears to contain players' IP addresses - " +
                        "please consider deleting your uploaded log, " +
                        "and posting it again with the IP addresses removed."
            )
        }

        return messages
    }
}
