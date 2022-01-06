package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue
import org.quiltmc.community.database.collections.FilterCollection
import org.quiltmc.community.database.entities.FilterEntry

suspend fun v10(db: CoroutineDatabase) {
    with(db.getCollection<FilterEntry>(FilterCollection.name)) {
        updateMany(
            FilterEntry::note exists false,
            setValue(FilterEntry::note, null),
        )
    }
}
