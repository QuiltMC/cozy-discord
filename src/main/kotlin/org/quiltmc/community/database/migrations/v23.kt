/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setTo
import org.quiltmc.community.database.collections.AmaConfigCollection
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.database.entities.UserFlags

suspend fun v23(db: CoroutineDatabase) {
	db.createCollection(AmaConfigCollection.name)
	db.getCollection<UserFlagsCollection>(UserFlagsCollection.name).updateMany(
		UserFlags::usePKFronter.exists(false),
		UserFlags::usePKFronter setTo false
	)
}
