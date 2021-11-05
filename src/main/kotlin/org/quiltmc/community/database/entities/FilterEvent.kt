@file:UseSerializers(UUIDSerializer::class)

@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.github.jershell.kbson.UUIDSerializer
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.database.Entity
import java.util.*

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class FilterEvent(
    override val _id: UUID = UUID.randomUUID(),
    val filter: UUID,

    val guildId: Snowflake,
    val authorId: Snowflake,
    val channelId: Snowflake,
    val messageId: Snowflake,
) : Entity<UUID>
