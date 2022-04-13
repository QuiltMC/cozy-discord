/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:UseSerializers(UUIDSerializer::class)

@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.github.jershell.kbson.UUIDSerializer
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.cozy.modules.tags.data.Tag
import org.quiltmc.community.database.Entity

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class TagEntity(
    override val _id: String,

    val category: String,
    val description: String,
    val title: String,

    val color: Color? = null,
    val guildId: Snowflake? = null,
    val image: String? = null
) : Entity<String> {
    fun toTag(): Tag = Tag(
        category = category,
        description = description,
        key = _id,
        title = title,
        color = color,
        guildId = guildId,
        image = image
    )

    companion object {
        fun fromTag(tag: Tag): TagEntity =
            TagEntity(
                _id = tag.key,

                category = tag.category,
                description = tag.description,
                title = tag.title,
                color = tag.color,
                guildId = tag.guildId,
                image = tag.image
            )
    }
}
