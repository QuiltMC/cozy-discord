/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database

import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.core.behavior.GuildBehavior
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.entities.ServerSettings

suspend fun GuildBehavior.getSettings(): ServerSettings? {
	val settingsColl = getKoin().get<ServerSettingsCollection>()

	return settingsColl.get(id)
}
