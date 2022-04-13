/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalGuild
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.FilterStrategy
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.suggestString
import dev.kord.rest.builder.message.create.allowedMentions
import org.koin.core.component.inject
import org.quiltmc.community.cozy.modules.tags.config.TagsConfig
import org.quiltmc.community.cozy.modules.tags.data.Tag
import org.quiltmc.community.cozy.modules.tags.data.TagsData

@Suppress("MagicNumber")
public class TagsExtension : Extension() {
    override val name: String = "quiltmc-tags"

    public val tagsConfig: TagsConfig by inject()
    public val tagsData: TagsData by inject()

    override suspend fun setup() {
        publicSlashCommand(::GetTagArgs) {
            name = "tag"
            description = "Retrieve a tag and send it"

            tagsConfig.getUserCommandChecks().forEach(::check)

            action {
                val tag = tagsData.getTagByKey(arguments.tagKey, guild?.id)

                if (tag == null) {
                    respond {
                        content = "Unknown tag: ${arguments.tagKey}"
                    }

                    return@action
                }

                respond {
                    tagsConfig.getTagFormatter()
                        .invoke(this, tag)
                }
            }
        }

        ephemeralSlashCommand {
            name = "list-tags"
            description = "Commands for listing tags by various criteria"

            tagsConfig.getUserCommandChecks().forEach(::check)

            ephemeralSubCommand(::ByCategoryArgs) {
                name = "by-category"
                description = "List tags by matching their category"

                action {
                    val tags = tagsData.getTagsByCategory(arguments.category, guild?.id)

                    if (tags.isEmpty()) {
                        respond {
                            content = "Tag not found"
                        }

                        return@action
                    }

                    editingPaginator {
                        tags.forEach { tag ->
                            page {
                                title = tag.title
                                description = tag.description
                                color = tag.color

                                footer {
                                    text = "${tag.category}/${tag.key}"
                                }
                            }
                        }
                    }.send()
                }
            }

            ephemeralSubCommand(::ByKeyArgs) {
                name = "by-key"
                description = "List tags by matching their key"

                action {
                    val tags = tagsData.getTagsByPartialKey(arguments.key, guild?.id)

                    if (tags.isEmpty()) {
                        respond {
                            content = "Tag not found"
                        }

                        return@action
                    }

                    editingPaginator {
                        tags.forEach { tag ->
                            page {
                                title = "tag.title"
                                description = tag.description
                                color = tag.color

                                footer {
                                    text = "${tag.category}/${tag.key}"
                                }
                            }
                        }
                    }.send()
                }
            }

            ephemeralSubCommand(::ByTitleArgs) {
                name = "by-title"
                description = "List tags by matching their title"

                action {
                    val tags = tagsData.getTagsByPartialTitle(arguments.title, guild?.id)

                    if (tags.isEmpty()) {
                        respond {
                            content = "Tag not found"
                        }

                        return@action
                    }

                    editingPaginator {
                        tags.forEach { tag ->
                            page {
                                title = "tag.title"
                                description = tag.description
                                color = tag.color

                                footer {
                                    text = "${tag.category}/${tag.key}"
                                }
                            }
                        }
                    }.send()
                }
            }
        }

        ephemeralSlashCommand {
            name = "manage-tags"
            description = "Tag management commands"

            check {
                anyGuild()
            }

            tagsConfig.getStaffCommandChecks().forEach(::check)

            ephemeralSubCommand(::SetArgs) {
                name = "set"
                description = "Create or update a tag"

                action {
                    val tag = Tag(
                        category = arguments.category,
                        description = arguments.description,
                        key = arguments.key,
                        title = arguments.title,
                        color = arguments.colour,
                        guildId = arguments.guild?.id
                    )

                    tagsData.setTag(tag)

                    tagsConfig.getLoggingChannel(guild!!.asGuild()).createMessage {
                        allowedMentions { }

                        content = "**Tag created/updated by ${user.mention}**\n\n"

                        tagsConfig.getTagFormatter().invoke(this, tag)
                    }

                    respond {
                        content = "Tag set: ${tag.title}"
                    }
                }
            }

            ephemeralSubCommand(::FindArgs) {
                name = "find"
                description = "Find tags, by the given key and guild ID"

                action {
                    val tags = tagsData.findTags(
                        category = arguments.category,
                        guildId = arguments.guild?.id,
                        key = arguments.key
                    )

                    if (tags.isEmpty()) {
                        respond {
                            content = "No tags found for that query."
                        }

                        return@action
                    }

                    editingPaginator {
                        val chunks = tags.chunked(10)

                        chunks.forEach { chunk ->
                            page {
                                description = chunk.joinToString("\n\n") {
                                    """
                                        **Key:** `${it.key}`
                                        **Title:** `${it.title}`
                                        **Category:** `${it.category}`
                                        **Guild ID:** `${it.guildId ?: "N/A"}`
                                    """.trimIndent()
                                }
                            }
                        }
                    }.send()
                }
            }

            ephemeralSubCommand(::ByKeyAndOptionalGuildArgs) {
                name = "delete"
                description = "Delete a tag, by key and guild ID"

                action {
                    val tag = tagsData.deleteTagByKey(arguments.key, arguments.guild?.id)

                    if (tag != null) {
                        tagsConfig.getLoggingChannel(guild!!.asGuild()).createMessage {
                            allowedMentions { }

                            content = "**Tag removed by ${user.mention}**\n\n"

                            tagsConfig.getTagFormatter().invoke(this, tag)
                        }
                    }

                    respond {
                        content = if (tag == null) {
                            "Tag not found"
                        } else {
                            "Deleted tag: ${tag.title}"
                        }
                    }
                }
            }
        }
    }

    private fun GetTagArgs(): GetTagArgs =
        GetTagArgs(tagsData)

    private fun ByCategoryArgs(): ByCategoryArgs =
        ByCategoryArgs(tagsData)

    private fun SetArgs(): SetArgs =
        SetArgs(tagsData)

    internal class ByKeyAndOptionalGuildArgs : Arguments() {
        val key by string {
            name = "key"
            description = "Tag key to match by"
        }

        val guild by optionalGuild {
            name = "guild"
            description = "Optional guild to match by - \"this\" for the current guild"
        }
    }

    internal class FindArgs : Arguments() {
        val category by optionalString {
            name = "category"
            description = "Optional category to match by"
        }
        val key by optionalString {
            name = "key"
            description = "Optional tag key to match by"
        }

        val guild by optionalGuild {
            name = "guild"
            description = "Optional guild to match by - \"this\" for the current guild"
        }
    }

    internal class SetArgs(tagsData: TagsData) : Arguments() {
        val key by string {
            name = "key"
            description = "Unique tag key"
        }

        val title by string {
            name = "title"
            description = "Tag title for display"
        }

        val description by string {
            name = "description"
            description = "Tag content - use \\n for a newline if needed"

            mutate {
                it.replace("\\n", "\n")
            }
        }

        val category by string {
            name = "category"
            description = "Category to use for this tag - specify a new one to create it"

            autoComplete {
                val categories = tagsData.getAllCategories(data.guildId.value)

                suggestStringMap(
                    categories.associateWith { it },
                    FilterStrategy.Contains
                )
            }
        }

        val colour by optionalColor {
            name = "colour"
            description = "Optional embed colour - use hex codes, RGB integers or Discord colour constants"
        }

        val guild by optionalGuild {
            name = "guild"
            description = "Optional guild to limit the tag to - \"this\" for the current guild"
        }
    }

    internal class ByCategoryArgs(tagsData: TagsData) : Arguments() {
        val category by string {
            name = "category"
            description = "Category to match by"

            autoComplete {
                val categories = tagsData.getAllCategories(data.guildId.value)

                suggestStringMap(
                    categories.associateWith { it },
                    FilterStrategy.Contains
                )
            }
        }
    }

    internal class ByKeyArgs : Arguments() {
        val key by string {
            name = "key"
            description = "Partial key to match by"
        }
    }

    internal class ByTitleArgs : Arguments() {
        val title by string {
            name = "title"
            description = "Partial title to match by"
        }
    }

    internal class GetTagArgs(tagsData: TagsData) : Arguments() {
        val tagKey by string {
            name = "tag"
            description = "Tag to retrieve"

            autoComplete {
                val input = focusedOption.value

                var category: String? = null
                lateinit var tagKey: String

                if ("/" in input) {
                    category = input.substringBeforeLast("/")
                    tagKey = input.substringAfterLast("/")
                } else {
                    tagKey = input
                }

                var potentialTags = (
                        tagsData.getTagsByPartialKey(tagKey, this.data.guildId.value) +
                                tagsData.getTagsByPartialTitle(tagKey, this.data.guildId.value)
                        )
                    .toSet()
                    .toList()

                if (category != null) {
                    potentialTags = potentialTags.filter { it.category.startsWith(category!!, true) }
                }

                val foundKeys: MutableList<String> = mutableListOf()

                potentialTags = potentialTags
                    .sortedBy { if (it.guildId == null) -1 else 1 }
                    .filter {
                        if (it.key !in foundKeys) {
                            foundKeys.add(it.key)

                            true
                        } else {
                            false
                        }
                    }

                potentialTags = potentialTags
                    .sortedBy { it.title }
                    .take(25)

                suggestString {
                    potentialTags.forEach {
                        choice("(${it.category}/${it.key}) ${it.title}", it.key)
                    }
                }
            }
        }
    }
}
