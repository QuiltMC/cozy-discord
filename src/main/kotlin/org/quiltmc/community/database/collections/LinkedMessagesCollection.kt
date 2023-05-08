/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.koin.core.component.inject
import org.litote.kmongo.contains
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.LinkedMessages

class LinkedMessagesCollection : KordExKoinComponent {
	private val database: Database by inject()
	private val col = database.mongo.getCollection<LinkedMessages>(name)

	suspend fun getBySource(id: Snowflake) =
		col.findOne(LinkedMessages::_id eq id)

	fun getByTarget(id: Snowflake) =
		col.find(LinkedMessages::targets contains id).toFlow()

	suspend fun set(linkedMessages: LinkedMessages) =
		col.save(linkedMessages)

	companion object : Collection("linked-messages")
}
