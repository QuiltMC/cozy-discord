package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.UserFlags
import org.quiltmc.community.github.DatabaseId

class UserFlagsCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<UserFlags>(name)

    suspend fun get(id: Snowflake) =
        col.findOne(UserFlags::_id eq id)

    suspend fun getByGithubId(id: DatabaseId) = col.findOne(UserFlags::githubId eq id)

    suspend fun getOrCreate(id: Snowflake) = col.findOne(UserFlags::_id eq id) ?: UserFlags(id)

    suspend fun set(userFlags: UserFlags) =
        col.save(userFlags)

    companion object : Collection("user-flags")
}
