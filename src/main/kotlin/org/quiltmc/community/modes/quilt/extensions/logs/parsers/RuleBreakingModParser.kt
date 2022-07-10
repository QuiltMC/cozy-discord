/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.parsers

private val INCOMPATIBLE_MODS = mapOf(
    "fabric_hider" to "Fabric Hider",
)

private val MATCH_REGEX = "- (${INCOMPATIBLE_MODS.keys.joinToString("|")})".toRegex(RegexOption.IGNORE_CASE)
private const val SITE_LINK = "https://quiltmc.org/community/rules/#5-keep-all-projects-legal-legitimate--appropriate"

class RuleBreakingModParser : BaseLogParser {
    override suspend fun getMessages(logContent: String): List<String> {
        val messages: MutableList<String> = mutableListOf()
        val matches = MATCH_REGEX.findAll(logContent).toList()

        if (matches.size < 2) {
            return messages
        }

        messages.add(
            buildString {
                append("You appear to have the following potentially rule-breaking mods installed: ")

                appendLine(
                    matches.subList(1, matches.size)
                        .map { INCOMPATIBLE_MODS[it.groupValues[1]] }
                        .toSet()
                        .sortedBy { it }
                        .joinToString { "**$it**" }
                )

                appendLine()
                append(
                    "For more information, please see [rule 5 on the site]($SITE_LINK). Please note that we cannot " +
                            "provide you with support while you're using mods that break our rules."
                )
            }
        )

        return messages
    }
}
