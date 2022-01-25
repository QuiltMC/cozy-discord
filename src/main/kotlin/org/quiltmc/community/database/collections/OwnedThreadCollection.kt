/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.threads.ThreadChannelBehavior
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.OwnedThread

class OwnedThreadCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<OwnedThread>(name)

    // region: Basic functions

    suspend fun get(id: Snowflake) =
        col.findOne(OwnedThread::_id eq id)

    suspend fun get(thread: ThreadChannelBehavior) =
        get(thread.id)

    suspend fun set(thread: OwnedThread) =
        col.save(thread)

    // endregion

    // region: Get by owner

    suspend fun getByOwner(id: Snowflake) =
        col.find(OwnedThread::owner eq id)

    suspend fun getByOwner(user: UserBehavior) =
        getByOwner(user.id)

    // endregion

    // region: Get by guild

    suspend fun getByGuild(id: Snowflake) =
        col.find(OwnedThread::guild eq id)

    suspend fun getByGuild(guild: GuildBehavior) =
        getByGuild(guild.id)

    // endregion

    // region: Get by owner and guild

    suspend fun getByOwnerAndGuild(owner: Snowflake, guild: Snowflake) =
        col.find(OwnedThread::owner eq owner, OwnedThread::guild eq guild)

    suspend fun getByOwnerAndGuild(owner: UserBehavior, guild: Snowflake) =
        getByOwnerAndGuild(owner.id, guild)

    suspend fun getByOwnerAndGuild(owner: Snowflake, guild: GuildBehavior) =
        getByOwnerAndGuild(owner, guild.id)

    suspend fun getByOwnerAndGuild(owner: UserBehavior, guild: GuildBehavior) =
        getByOwnerAndGuild(owner.id, guild.id)

    // endregion

    // region: Is owner

    suspend fun isOwner(thread: Snowflake, user: Snowflake) =
        get(thread)?.let { it.owner == user }

    suspend fun isOwner(thread: Snowflake, user: UserBehavior) =
        isOwner(thread, user.id)

    suspend fun isOwner(thread: ThreadChannelBehavior, user: Snowflake) =
        isOwner(thread.id, user)

    suspend fun isOwner(thread: ThreadChannelBehavior, user: UserBehavior) =
        isOwner(thread.id, user.id)

    // endregion

    companion object : Collection("owned-threads")
}
