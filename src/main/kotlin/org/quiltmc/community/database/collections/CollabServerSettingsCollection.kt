package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.CollabServerSettings

class CollabServerSettingsCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<CollabServerSettings>(name)

    suspend fun get(id: Snowflake) =
        col.findOne(CollabServerSettings::_id eq id)

    suspend fun set(settings: CollabServerSettings) =
        col.save(settings)

    suspend fun delete(id: Snowflake) =
        col.deleteOne(CollabServerSettings::_id eq id)

    suspend fun delete(settings: CollabServerSettings) =
        delete(settings._id)

    companion object : Collection("collab-server-settings")
}
