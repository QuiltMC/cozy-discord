package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.ServerSettings
import org.quiltmc.community.database.enums.QuiltServerType

class ServerSettingsCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<ServerSettings>(name)

    suspend fun get(id: Snowflake) =
        col.findOne(ServerSettings::_id eq id)

    suspend fun getByServerType(type: QuiltServerType?) =
        col.find(ServerSettings::quiltServerType eq type)

    suspend fun getCommunity() =
        col.findOne(ServerSettings::quiltServerType eq QuiltServerType.COMMUNITY)

    suspend fun getToolchain() =
        col.findOne(ServerSettings::quiltServerType eq QuiltServerType.TOOLCHAIN)

    suspend fun set(settings: ServerSettings) =
        col.save(settings)

    suspend fun delete(id: Snowflake) =
        col.deleteOne(ServerSettings::_id eq id)

    suspend fun delete(settings: ServerSettings) =
        delete(settings._id)

    companion object : Collection("server-settings")
}
