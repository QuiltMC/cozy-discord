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
import org.quiltmc.community.database.entities.Team

class TeamCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<Team>(name)

    suspend fun get(id: Snowflake) =
        col.findOne(Team::_id eq id)

    suspend fun set(team: Team) =
        col.save(team)

    suspend fun delete(id: Snowflake) =
        col.deleteOne(Team::_id eq id)

    suspend fun getImmediateChildren(id: Snowflake) =
        col.find(Team::parent eq id)

    suspend fun getParents(id: Snowflake) =
        col.aggregate<AggregateResult>(
            listOf(
                graphLookup(
                    name,
                    id,
                    "parent",
                    "_id",
                    "parentHierarchy"
                ),

                limit(1)
            )
        ).first()?.parentHierarchy?.map { it.parent }.orEmpty()

    @Serializable
    data class AggregateResult(val parentHierarchy: List<Team>)

    companion object : Collection("teams")
}
