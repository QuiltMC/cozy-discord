package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.database.collections.UserFlagsCollection

suspend fun v9(db: CoroutineDatabase) {
    db.createCollection(UserFlagsCollection.name)
}
