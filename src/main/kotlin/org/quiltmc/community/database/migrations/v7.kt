package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.database.collections.GlobalSettingsCollection

suspend fun v7(db: CoroutineDatabase) {
    db.dropCollection("collab-server-settings")
    db.createCollection(GlobalSettingsCollection.name)
}
