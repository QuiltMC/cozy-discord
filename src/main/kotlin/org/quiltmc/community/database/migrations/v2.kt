package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue
import org.quiltmc.community.database.collections.SuggestionsCollection
import org.quiltmc.community.database.entities.Suggestion

suspend fun v2(db: CoroutineDatabase) {
    db.createCollection("collab-server-settings")

    with(db.getCollection<Suggestion>(SuggestionsCollection.name)) {
        updateMany(
            Suggestion::thread exists false,
            setValue(Suggestion::thread, null),
        )

        updateMany(
            Suggestion::threadButtons exists false,
            setValue(Suggestion::threadButtons, null),
        )
    }
}
