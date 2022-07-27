/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue
import org.quiltmc.community.database.collections.FilterCollection
import org.quiltmc.community.database.entities.FilterEntry
import org.quiltmc.community.modes.quilt.extensions.filtering.MatchTarget

suspend fun v12(db: CoroutineDatabase) {
	with(db.getCollection<FilterEntry>(FilterCollection.name)) {
		updateMany(
			FilterEntry::matchTarget exists false,
			setValue(FilterEntry::matchTarget, MatchTarget.MESSAGE),
		)
	}
}
