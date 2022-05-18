/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.database.entities.UserFlags

suspend fun v16(db: CoroutineDatabase) {
    with(db.getCollection<UserFlags>(UserFlagsCollection.name)) {
        updateMany(
                UserFlags::syncNicks exists false,
                setValue(UserFlags::syncNicks, true),
        )
    }
}
