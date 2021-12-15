package org.quiltmc.community.api.pluralkit

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PKMessage(
    val timestamp: Instant,
    val id: Snowflake,
    val original: Snowflake,
    val sender: Snowflake,
    val channel: Snowflake,

    val system: PKSystem,
    val member: PKMember,
)
