@file:Suppress("DataClassShouldBeImmutable", "DataClassContainsFunctions")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.github.DatabaseId

@Serializable
data class UserFlags(
    override val _id: Snowflake,

    var hasUsedPK: Boolean = false,
    var autoPublish: Boolean = true,
    var githubId: DatabaseId? = null
) : Entity<Snowflake> {
    suspend fun save() {
        val collection = getKoin().get<UserFlagsCollection>()

        collection.set(this)
    }
}
