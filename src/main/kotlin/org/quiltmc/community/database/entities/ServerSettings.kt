package org.quiltmc.community.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class ServerSettings(
    val _id: Snowflake,

    val banSyncChannel: Snowflake? = null,
    val commandPrefix: String? = null,

    val managers: MutableList<Snowflake> = mutableListOf(),
)
