package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.quiltmc.community.database.collections.TeamCollection

suspend fun v4(db: CoroutineDatabase) {
    db.createCollection(TeamCollection.name)
}
