/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.loadModule
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

    val messageEmbedBuilders = other.embeds.map { embed ->
        EmbedBuilder().also {
            embed.apply(it)
        }
    }

    return content == other.content &&
            embeds.size == messageEmbedBuilders.size &&
            embeds.all { embed ->
                messageEmbedBuilders.any { otherEmbed ->
                    embed.isSimilar(otherEmbed)
                }
            }
}

public fun EmbedBuilder.isSimilar(other: EmbedBuilder): Boolean {
    return title == other.title &&
            description == other.description &&
            footer?.text == other.footer?.text &&
            footer?.icon == other.footer?.icon &&
            image == other.image &&
            thumbnail?.url == other.thumbnail?.url &&
            color == other.color &&
            timestamp == other.timestamp &&
            author?.icon == other.author?.icon &&
            author?.url == other.author?.url &&
            author?.name == other.author?.name &&

            fields.all { field ->
                other.fields.any { otherField ->
                    field.inline == otherField.inline &&
                            field.value == otherField.value &&
                            field.name == otherField.name
                }
            }
}
