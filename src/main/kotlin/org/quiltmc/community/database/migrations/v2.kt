/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue
import org.quiltmc.community.database.collections.SuggestionsCollection
import org.quiltmc.community.database.entities.Suggestion

suspend fun v2(db: CoroutineDatabase) {
    db.createCollection("collab-server-settings")

    with(db.getCollection<Suggestion>(SuggestionsCollection.name)) {
        updateMany(
            Suggestion::thread exists false,
            setValue(Suggestion::thread, null),
        )

        updateMany(
            Suggestion::threadButtons exists false,
            setValue(Suggestion::threadButtons, null),
        )
    }
}
