/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import org.koin.core.component.inject
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.Meta

class MetaCollection : KordExKoinComponent {
	private val database: Database by inject()
	private val col = database.mongo.getCollection<Meta>(name)

	suspend fun get() =
		col.findOne()

	suspend fun set(meta: Meta) =
		col.save(meta)

	companion object : Collection("meta")
}
