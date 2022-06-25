/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.cozy.modules.welcome.data.WelcomeChannelData
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.WelcomeChannelEntity

class WelcomeChannelCollection : KordExKoinComponent, WelcomeChannelData {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<WelcomeChannelEntity>(name)

    override suspend fun getChannelURLs(): Map<Snowflake, String> =
        col.find()
            .toList()
            .map { it._id to it.url }
            .toMap()

    override suspend fun getUrlForChannel(channelId: Snowflake): String? =
        col.findOne(WelcomeChannelEntity::_id eq channelId)
            ?.url

    override suspend fun setUrlForChannel(channelId: Snowflake, url: String) {
        col.save(WelcomeChannelEntity(channelId, url))
    }

    override suspend fun removeChannel(channelId: Snowflake): String? {
        val url = getUrlForChannel(channelId)
            ?: return null

        col.deleteOne(WelcomeChannelEntity::_id eq channelId)

        return url
    }

    companion object : Collection("welcome-channels")
}
