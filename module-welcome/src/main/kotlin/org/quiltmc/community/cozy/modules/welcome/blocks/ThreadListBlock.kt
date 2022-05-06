/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.blocks

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import dev.kord.common.Color
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.quiltmc.community.cozy.modules.welcome.enums.ThreadListType
import org.quiltmc.community.cozy.modules.welcome.getJumpUrl

@Suppress("DataClassContainsFunctions")
@Serializable
@SerialName("threads")
public data class ThreadListBlock(
    val mode: ThreadListType,
    val limit: Int = 10,

    val text: String? = null,
    val description: String? = null,
    val color: Color = DISCORD_BLURPLE,
    val title: String = "${mode.humanReadable} Threads",
    val template: String = "**»** [{NAME}]({URL})",

    @SerialName("active_emoji")
    val activeEmoji: String? = null,

    @SerialName("archived_emoji")
    val archivedEmoji: String? = null,

    @SerialName("archive_status_in_name")
    val archiveStatusInName: Boolean = true,

    @SerialName("include_archived")
    val includeArchived: Boolean = true,

    @SerialName("include_news")
    val includeNews: Boolean = true,

    @SerialName("include_public")
    val includePublic: Boolean = true,

    @SerialName("include_private")
    val includePrivate: Boolean = false,

    @SerialName("include_hidden_channels")
    val includeHiddenChannels: Boolean = false,
) : Block() {
    override suspend fun create(builder: MessageCreateBuilder) {
        builder.content = text

        builder.embed { applyThreads() }
    }

    override suspend fun edit(builder: MessageModifyBuilder) {
        builder.content = text
        builder.components = mutableListOf()

        builder.embed { applyThreads() }
    }

    private suspend fun EmbedBuilder.applyThreads() {
        val threads = getThreads()

        this.color = this@ThreadListBlock.color
        this.title = this@ThreadListBlock.title

        description = buildString {
            if (this@ThreadListBlock.description != null) {
                append(this@ThreadListBlock.description)
                append("\n\n")
            }

            threads.forEach { thread ->
                var line = template
                    .replace("{MENTION}", thread.mention)
                    .replace("{URL}", thread.getJumpUrl())
                    .replace("{CREATED_TIME}", thread.id.timestamp.toDiscord(TimestampType.RelativeTime))
                    .replace("{PARENT_ID}", thread.parentId.toString())
                    .replace("{PARENT}", thread.parent.mention)

                if (thread.lastMessageId != null) {
                    line = line.replace(
                        "{ACTIVE_TIME}",
                        thread.lastMessageId!!.timestamp.toDiscord(TimestampType.RelativeTime)
                    )
                }

                line = if (archiveStatusInName && thread.isArchived) {
                    line.replace("{NAME}", thread.name + " (Archived)")
                } else {
                    line.replace("{NAME}", thread.name)
                }

                line = when {
                    thread.isArchived && archivedEmoji != null -> line.replace("{EMOJI}", archivedEmoji)
                    thread.isArchived.not() && activeEmoji != null -> line.replace("{EMOJI}", activeEmoji)

                    else -> line.replace("{EMOJI}", "")
                }

                appendLine(line)
            }
        }
    }

    private suspend fun getThreads(): List<ThreadChannel> {
        var threads = guild.threads
            .filter { thread ->
                if (!includeHiddenChannels) {
                    val channel = thread.parent.asChannelOfOrNull<TextChannel>()

                    if (channel == null) {
                        false
                    } else {
                        val overwrite = channel.permissionOverwrites.firstOrNull { it.target == guild.id }

                        overwrite == null || overwrite.denied.contains(Permission.ViewChannel).not()
                    }
                } else {
                    true
                }
            }
            .toList()

        threads = when (mode) {
            ThreadListType.ACTIVE -> threads.sortedByDescending { it.lastMessage?.id?.timestamp }
            ThreadListType.NEWEST -> threads.sortedByDescending { it.id.timestamp }
        }

        if (!includeArchived) {
            threads = threads.filter { !it.isArchived }
        }

        if (!includeNews) {
            threads = threads.filter { it.type != ChannelType.PublicNewsThread }
        }

        if (!includePublic) {
            threads = threads.filter { it.type != ChannelType.PublicGuildThread }
        }

        if (!includePrivate) {
            threads = threads.filter { it.type != ChannelType.PrivateThread }
        }

        return threads.take(limit)
    }
}
