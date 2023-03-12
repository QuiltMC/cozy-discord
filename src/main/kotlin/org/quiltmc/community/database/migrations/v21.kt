/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.exists
import org.litote.kmongo.setValue
import org.quiltmc.community.database.collections.ServerApplicationCollection
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.entities.ServerApplication
import org.quiltmc.community.database.entities.ServerSettings

suspend fun v21(db: CoroutineDatabase) {
	val settingsCollection = db.getCollection<ServerSettings>(ServerSettingsCollection.name)
	val appCollection = db.getCollection<ServerApplication>(ServerApplicationCollection.name)

	val entries = appCollection.find(
		ServerApplication::messageLink exists false,
	).toList()

	entries
		.groupBy { it.guildId }
		.forEach { (guildId, applications) ->
			val settings = settingsCollection.findOne(ServerSettings::_id eq guildId)!!

			applications.forEach { app ->
				val link = "https://discord.com/channels" +
						"/${settings._id}" +
						"/${settings.applicationLogChannel}" +
						"/${app.messageId}"

				appCollection.updateOne(
					ServerApplication::_id eq app._id,
					setValue(ServerApplication::messageLink, link),
				)
			}
		}
}
