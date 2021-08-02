package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.collections.SuggestionsCollection

suspend fun v1(db: CoroutineDatabase) {
    db.createCollection(ServerSettingsCollection.name)
    db.createCollection(SuggestionsCollection.name)
}
