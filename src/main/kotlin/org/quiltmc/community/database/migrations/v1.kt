package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun v1(db: CoroutineDatabase) {
    db.createCollection("server-settings")
    db.createCollection("suggestions")
}
