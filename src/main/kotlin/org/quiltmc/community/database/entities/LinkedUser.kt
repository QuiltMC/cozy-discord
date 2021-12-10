package org.quiltmc.community.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity
import org.quiltmc.community.github.NodeId


@Serializable

data class LinkedUser(
    override val _id: Snowflake,
    val githubId: NodeId
) : Entity<Snowflake>
