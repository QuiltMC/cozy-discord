@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity
import org.quiltmc.community.github.NodeId

@Serializable
/**
 * A team with both a Discord role and a GitHub presence. The documentation for this class uses the GitHub v4 (GraphQL) API terminology.
 */
data class Team(

    override val _id: Snowflake,
    /**
     * The teams parents, which **may be different from its ancestors on GitHub**. The parents determine
     * which teams may add and remove members from this team. A team may be a parent of itself, but this is not recommended.
     */
    val parents: Collection<Snowflake>,
    /**
     * The node ID of this team on GitHub.
     */
    val nodeId: NodeId
) : Entity<Snowflake>
