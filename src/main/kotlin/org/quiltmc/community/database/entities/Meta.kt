package org.quiltmc.community.database.entities

import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class Meta(
    val version: Int,

    override val _id: String = "meta"  // Should never change, we only want one instance of it
) : Entity<String>
