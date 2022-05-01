/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs

import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.MessageType
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import org.quiltmc.community.inQuiltGuild
import org.quiltmc.community.modes.quilt.extensions.logs.parsers.BaseLogParser
import org.quiltmc.community.modes.quilt.extensions.logs.parsers.FabricImplParser
import org.quiltmc.community.modes.quilt.extensions.logs.parsers.FabricLoaderParser
import org.quiltmc.community.modes.quilt.extensions.logs.parsers.LoaderVersionParser
import org.quiltmc.community.modes.quilt.extensions.logs.retrievers.AttachmentLogRetriever
import org.quiltmc.community.modes.quilt.extensions.logs.retrievers.BaseLogRetriever
import org.quiltmc.community.modes.quilt.extensions.logs.retrievers.RawLogRetriever
import org.quiltmc.community.modes.quilt.extensions.logs.retrievers.ScrapingLogRetriever

class LogParsingExtension : Extension() {
    override val name: String = "log-parsing"

    private val parsers: List<BaseLogParser> = listOf(
        FabricLoaderParser(),
        FabricImplParser(),
        LoaderVersionParser(),
    )

    private val retrievers: List<BaseLogRetriever> = listOf(
        AttachmentLogRetriever(),
        RawLogRetriever(),
        ScrapingLogRetriever(),
    )

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { inQuiltGuild() }
            check {
                failIfNot(event.message.type in arrayOf(MessageType.Default, MessageType.Reply))
            }

            action {
                val logs = retrievers
                    .map { it.getLogContent(event.message) }
                    .reduceOrNull { left, right -> left + right }

                val messages = logs
                    ?.map { log ->
                        parsers
                            .map { it.getMessages(log) }
                            .reduce { left, right -> left + right }
                    }
                    ?.reduceOrNull { left, right -> left + right }
                    ?.map { "**»** $it" }
                    ?.toSet()
                    ?: setOf()

                if (messages.isNotEmpty()) {
                    event.message.respond {
                        embed {
                            title = "Automatic log analysis"

                            color = DISCORD_FUCHSIA

                            description = "**The following potential issues were found in your logs:**\n\n" +
                                    messages.joinToString("\n\n")

                            @Suppress("MagicNumber")
                            if (description!!.length > 4000) {
                                description = description!!.slice(0 until 3997) + "..."
                            }
                        }
                    }
                }
            }
        }
    }
}
