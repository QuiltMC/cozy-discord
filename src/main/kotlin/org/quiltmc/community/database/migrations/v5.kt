package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.database.collections.FilterCollection

suspend fun v5(db: CoroutineDatabase) {
    db.createCollection(FilterCollection.name)
}
