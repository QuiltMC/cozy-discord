package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.graphLookup
import org.litote.kmongo.limit
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.LinkedUser
import org.quiltmc.community.database.entities.Team

class LinkedUserCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<LinkedUser>(name)

    suspend fun get(id: Snowflake) =
        col.findOne(LinkedUser::_id eq id)

    suspend fun set(user: LinkedUser) =
        col.save(user)

    suspend fun delete(id: Snowflake) =
        col.deleteOne(LinkedUser::_id eq id)

    @Serializable
    data class AggregateResult(val parentHierarchy: List<Team>)

    companion object : Collection("linked-users")
}
