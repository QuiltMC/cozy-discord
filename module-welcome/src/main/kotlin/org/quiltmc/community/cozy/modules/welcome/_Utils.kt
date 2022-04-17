/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DEPRECATION")

package org.quiltmc.community.cozy.modules.welcome

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.entity.EmbedType
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import org.koin.dsl.bind
import org.quiltmc.community.cozy.modules.welcome.config.SimpleWelcomeChannelConfig
import org.quiltmc.community.cozy.modules.welcome.config.WelcomeChannelConfig
import org.quiltmc.community.cozy.modules.welcome.data.WelcomeChannelData

public fun ExtensibleBotBuilder.ExtensionsBuilder.welcomeChannel(
    config: WelcomeChannelConfig,
    data: WelcomeChannelData
) {
    loadModule { single { config } bind WelcomeChannelConfig::class }
    loadModule { single { data } bind WelcomeChannelData::class }

    add { WelcomeExtension() }
}

public fun ExtensibleBotBuilder.ExtensionsBuilder.welcomeChannel(
    data: WelcomeChannelData,
    body: SimpleWelcomeChannelConfig.Builder.() -> Unit
) {
    welcomeChannel(SimpleWelcomeChannelConfig(body), data)
}

public fun MessageCreateBuilder.isSimilar(other: Message): Boolean {
    if (components.isNotEmpty() || other.actionRows.isNotEmpty()) {
        // We do this because comparing components is a type-safety mess and a waste of time in comparison

        return false
    }

    val messageEmbedBuilders = other.embeds
        .filter { it.type == null || it.type == EmbedType.Rich }
        .map { embed ->
            EmbedBuilder().also {
                embed.apply(it)
            }
        }

    if (content == null) {
        content = ""
    }

    return content == other.content &&
            embeds.size == messageEmbedBuilders.size &&

            embeds.filterIndexed { index, embed ->
                val otherEmbed = messageEmbedBuilders[index]

                embed.isSimilar(otherEmbed)
            }.size == embeds.size
}

public fun EmbedBuilder.isSimilar(other: EmbedBuilder): Boolean {
    return title?.trim() == other.title?.trim() &&
            description?.trim() == other.description?.trim() &&
            footer?.text?.trim() == other.footer?.text?.trim() &&
            footer?.icon?.trim() == other.footer?.icon?.trim() &&
            image?.trim() == other.image?.trim() &&
            thumbnail?.url?.trim() == other.thumbnail?.url?.trim() &&
            author?.icon?.trim() == other.author?.icon?.trim() &&
            author?.url?.trim() == other.author?.url?.trim() &&
            author?.name?.trim() == other.author?.name?.trim() &&

            color == other.color &&
            timestamp == other.timestamp &&

            fields.all { field ->
                other.fields.any { otherField ->
                    field.inline == otherField.inline &&
                            field.value.trim() == otherField.value.trim() &&
                            field.name.trim() == otherField.name.trim()
                }
            }
}
