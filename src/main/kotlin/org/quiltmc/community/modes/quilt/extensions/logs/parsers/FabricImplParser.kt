/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.parsers

class FabricImplParser : BaseLogParser {
    override suspend fun getMessages(logContent: String): List<String> {
        var classNotFoundLine: Int? = null
        var suspectedMod: String? = null
        var suspectedPackage: String? = null

        for ((index, line) in logContent.lines().mapIndexed { index, s -> index to s }) {
            if (line.startsWith("java.lang.RuntimeException: Could not execute entrypoint stage")) {
                classNotFoundLine = index

                suspectedMod = line
                    .split("' due to errors, provided by '")
                    .lastOrNull()
                    ?.split("'")
                    ?.firstOrNull()
                    ?.trim()

                continue
            }

            if (classNotFoundLine == null || suspectedMod == null) {
                continue
            }

            if (line.startsWith("Caused by: java.lang.ClassNotFoundException:")) {
                suspectedPackage = line.split("ClassNotFoundException:").lastOrNull()?.trim()

                if (suspectedPackage != null) {
                    break
                }
            }
        }

        val messages: MutableList<String> = mutableListOf()

        if (
            suspectedMod != null &&
            suspectedPackage != null &&
            ".fabricmc." in suspectedPackage && ".impl." in suspectedPackage
        ) {
            messages.add(
                "Mod `$suspectedMod` may be using Fabric internals:\n`$suspectedPackage`"
            )
        }

        return messages
    }
}
