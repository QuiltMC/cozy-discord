package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue
import org.quiltmc.community.database.collections.OwnedThreadCollection
import org.quiltmc.community.database.entities.OwnedThread

suspend fun v8(db: CoroutineDatabase) {
    with(db.getCollection<OwnedThread>(OwnedThreadCollection.name)) {
        updateMany(
            OwnedThread::save exists false,
            setValue(OwnedThread::save, false),
        )
    }
}
