/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.ChannelBehavior
import org.koin.core.component.inject
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.FilterEntry
import org.quiltmc.community.database.entities.FilterEvent

class FilterEventCollection : KordExKoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<FilterEvent>(name)

    suspend fun add(
        filter: FilterEntry,

        guild: GuildBehavior,
        author: UserBehavior,
        channel: ChannelBehavior?,
        message: MessageBehavior?
    ) = add(
        FilterEvent(
            filter = filter._id,

            guildId = guild.id,
            authorId = author.id,
            channelId = channel?.id,
            messageId = message?.id
        )
    )

    suspend fun add(event: FilterEvent) =
        col.save(event)

    companion object : Collection("filter_events")
}
