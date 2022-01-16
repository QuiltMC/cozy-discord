@file:Suppress("DataClassShouldBeImmutable", "DataClassContainsFunctions")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity
import org.quiltmc.community.database.collections.TeamCollection
import org.quiltmc.community.github.DatabaseId

@Serializable
/**
 * A team with both a Discord role and a GitHub presence.
 */
data class Team(

    override val _id: Snowflake,
    /**
     * The team's managers, which **is usually different from its ancestors on GitHub**. The managers determine
     * which teams may add and remove members from this team. A team may be a manager of itself,
     * but this is usually not recommended. Admins are currently managers of every team.
     */
    val managers: MutableList<Snowflake>,
    /**
     * The database ID of this team on GitHub.
     */
    val databaseId: DatabaseId
) : Entity<Snowflake> {
    suspend fun save() {
        val collection = getKoin().get<TeamCollection>()

        collection.set(this)
    }
}
