package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.ServerSettings

class ServerSettingsCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<ServerSettings>("server-settings")

    suspend fun get(id: Snowflake) =
        col.findOne(ServerSettings::_id eq id)

    suspend fun set(settings: ServerSettings) =
        col.save(settings)

    suspend fun delete(id: Snowflake) =
        col.deleteOne(ServerSettings::_id eq id)

    suspend fun delete(settings: ServerSettings) =
        delete(settings._id)
}
