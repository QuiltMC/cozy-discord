package org.quiltmc.community.database

import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.core.behavior.GuildBehavior
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.entities.ServerSettings

suspend fun GuildBehavior.getSettings(): ServerSettings? {
    val settingsColl = getKoin().get<ServerSettingsCollection>()

    return settingsColl.get(id)
}
