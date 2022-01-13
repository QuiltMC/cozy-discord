@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity
import org.quiltmc.community.github.DatabaseId
import org.quiltmc.community.github.NodeId

@Serializable
/**
 * A team with both a Discord role and a GitHub presence.
 */
data class Team(

    override val _id: Snowflake,
    /**
     * The team's managers, which **may be different from its ancestors on GitHub**. The managers determine
     * which teams may add and remove members from this team. A team may be a manager of itself,
     * but this is usually not recommended.
     */
    val managers: Collection<Snowflake>,
    /**
     * The database ID of this team on GitHub.
     */
    val databaseId: DatabaseId
) : Entity<Snowflake>
