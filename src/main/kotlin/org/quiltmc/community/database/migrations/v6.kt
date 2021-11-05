package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.database.collections.FilterEventCollection

suspend fun v6(db: CoroutineDatabase) {
    db.createCollection(FilterEventCollection.name)
}
