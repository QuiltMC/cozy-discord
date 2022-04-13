/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import org.bson.conversions.Bson
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.or
import org.quiltmc.community.cozy.modules.tags.data.Tag
import org.quiltmc.community.cozy.modules.tags.data.TagsData
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.TagEntity

class TagsCollection : KoinComponent, TagsData {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<TagEntity>(name)

    override suspend fun getTagByKey(key: String, guildId: Snowflake?): Tag? =
        col.findOne(
            or(TagEntity::guildId eq null, TagEntity::guildId eq guildId),
            TagEntity::_id eq key.lowercase()
        )?.toTag()

    override suspend fun getTagsByCategory(category: String, guildId: Snowflake?): List<Tag> =
        col.find(
            or(TagEntity::guildId eq null, TagEntity::guildId eq guildId),
            TagEntity::category eq category.lowercase()
        )
            .toList()
            .map { it.toTag() }

    override suspend fun getTagsByPartialKey(partialKey: String, guildId: Snowflake?): List<Tag> =
        col.find(or(TagEntity::guildId eq null, TagEntity::guildId eq guildId))
            .toList()
            .filter { it._id.contains(partialKey, true) }
            .map { it.toTag() }

    override suspend fun getTagsByPartialTitle(partialTitle: String, guildId: Snowflake?): List<Tag> =
        col.find(or(TagEntity::guildId eq null, TagEntity::guildId eq guildId))
            .toList()
            .filter { it.title.contains(partialTitle, true) }
            .map { it.toTag() }

    override suspend fun getAllCategories(guildId: Snowflake?): Set<String> =
        col.distinct(
            TagEntity::category,
            or(TagEntity::guildId eq null, TagEntity::guildId eq guildId)
        ).toList().toSet()

    override suspend fun findTags(category: String?, guildId: Snowflake?, key: String?): List<Tag> {
        val criteria = mutableListOf<Bson>()

        if (category != null) {
            criteria.add(TagEntity::category eq category.lowercase())
        }

        if (guildId != null) {
            criteria.add(TagEntity::guildId eq guildId)
        }

        if (key != null) {
            criteria.add(TagEntity::_id eq key.lowercase())
        }

        @Suppress("SpreadOperator")
        val tags = col.find(
            *criteria.toTypedArray()
        ).toList()

        return tags.map { it.toTag() }
    }

    override suspend fun setTag(tag: Tag) {
        col.save(TagEntity.fromTag(tag))
    }

    override suspend fun deleteTagByKey(key: String, guildId: Snowflake?): Tag? {
        val tag = getTagByKey(key, guildId) ?: return null

        col.deleteOne(
            TagEntity::_id eq key,
            TagEntity::guildId eq guildId
        )

        return tag
    }

    companion object : Collection("tags")
}
