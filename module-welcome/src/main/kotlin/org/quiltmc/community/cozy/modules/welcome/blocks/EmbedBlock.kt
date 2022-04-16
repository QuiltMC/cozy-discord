/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.blocks

import dev.kord.core.Kord
import dev.kord.core.cache.data.EmbedData
import dev.kord.core.entity.Embed
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kord.rest.builder.message.modify.embed
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
@SerialName("embed")
public data class EmbedBlock(
    val embed: EmbedData,
    val text: String? = null
) : Block(), KoinComponent {
    val kord: Kord by inject()

    override suspend fun create(builder: MessageCreateBuilder) {
        builder.content = text

        builder.embed {
            Embed(embed, kord).apply(this)
        }
    }

    override suspend fun edit(builder: MessageModifyBuilder) {
        builder.content = text
        builder.components = mutableListOf()

        builder.embed {
            Embed(embed, kord).apply(this)
        }
    }
}
