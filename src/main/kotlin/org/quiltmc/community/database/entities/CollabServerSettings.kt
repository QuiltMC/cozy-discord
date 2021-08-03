@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class CollabServerSettings(
    override val _id: Snowflake,

    var banCommandTemplate: String? = null,
    var banSyncChannel: Snowflake? = null,

    var moderatorPerm: Permissions = Permissions(Permission.ManageGuild),
    val moderatorRoles: MutableList<Snowflake> = mutableListOf(),

    var isBlocklist: Boolean = true,
    val listedGuilds: MutableList<Snowflake> = mutableListOf(),
) : Entity<Snowflake>
