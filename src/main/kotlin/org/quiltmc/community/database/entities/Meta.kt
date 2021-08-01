package org.quiltmc.community.database.entities

import kotlinx.serialization.Serializable

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class Meta(
    val version: Int,

    val _id: String = "meta"  // Should never change, we only want one instance of it
)
