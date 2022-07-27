/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.MessageBehavior
import org.bson.conversions.Bson
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.Suggestion

class SuggestionsCollection : KordExKoinComponent {
	private val database: Database by inject()
	private val col = database.mongo.getCollection<Suggestion>(name)

	suspend fun get(id: Snowflake) =
		col.findOne(Suggestion::_id eq id)

	suspend fun getByMessage(id: Snowflake) =
		col.findOne(Suggestion::message eq id)

	suspend fun getByThread(id: Snowflake) =
		col.findOne(Suggestion::thread eq id)

	suspend fun getByMessage(message: MessageBehavior) =
		getByMessage(message.id)

	suspend fun find(filter: Bson) =
		col.find(filter)

	suspend fun set(suggestion: Suggestion) =
		col.save(suggestion)

	companion object : Collection("suggestions")
}
