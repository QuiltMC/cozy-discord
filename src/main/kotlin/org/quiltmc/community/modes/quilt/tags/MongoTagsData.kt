/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.tags

import dev.kord.common.entity.Snowflake
import org.quiltmc.community.cozy.modules.tags.data.Tag
import org.quiltmc.community.cozy.modules.tags.data.TagsData

class MongoTagsData : TagsData {
    override suspend fun getTagByKey(key: String, guildId: Snowflake?): Tag? {
        TODO("Not yet implemented")
    }

    override suspend fun getTagsByCategory(category: String, guildId: Snowflake?): List<Tag> {
        TODO("Not yet implemented")
    }

    override suspend fun getTagsByPartialKey(partialKey: String, guildId: Snowflake?): List<Tag> {
        TODO("Not yet implemented")
    }

    override suspend fun getTagsByPartialTitle(partialTitle: String, guildId: Snowflake?): List<Tag> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllCategories(guildId: Snowflake?): Set<String> {
        TODO("Not yet implemented")
    }

    override suspend fun findTags(category: String?, guildId: Snowflake?, key: String?): List<Tag> {
        TODO("Not yet implemented")
    }

    override suspend fun setTag(tag: Tag) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteTagByKey(key: String, guildId: Snowflake?): Tag? {
        TODO("Not yet implemented")
    }
}
