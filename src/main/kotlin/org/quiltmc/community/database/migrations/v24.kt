/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.migrations

import com.mongodb.client.model.Updates
import org.litote.kmongo.EMPTY_BSON
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.database.collections.ServerSettingsCollection

suspend fun v24(db: CoroutineDatabase) {
	db.getCollection<ServerSettingsCollection>(ServerSettingsCollection.name).updateMany(
		EMPTY_BSON,
		Updates.unset("verificationRole")
	)
}
