/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.ServerApplication

class ServerApplicationCollection : KordExKoinComponent {
	private val database: Database by inject()
	private val col = database.mongo.getCollection<ServerApplication>(name)

	suspend fun save(event: ServerApplication) =
		col.save(event)

	suspend fun get(id: Snowflake) =
		col.findOne(ServerApplication::_id eq id)

	suspend fun getByMessage(id: Snowflake) =
		col.findOne(ServerApplication::messageId eq id)

	fun findByGuild(id: Snowflake) =
		col.find(ServerApplication::guildId eq id)
			.toFlow()

	fun findByUser(id: Snowflake) =
		col.find(ServerApplication::userId eq id)
			.toFlow()

	companion object : Collection("server_applications")
}
