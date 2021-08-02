package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.database.collections.CollabServerSettingsCollection

suspend fun v2(db: CoroutineDatabase) {
    db.createCollection(CollabServerSettingsCollection.name)
}
