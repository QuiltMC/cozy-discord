package org.quiltmc.community.database.migrations

import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue
import org.quiltmc.community.database.collections.OwnedThreadCollection
import org.quiltmc.community.database.collections.TeamCollection
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.database.entities.OwnedThread
import org.quiltmc.community.database.entities.UserFlags

suspend fun v10(db: CoroutineDatabase) {
    db.dropCollection("teams")
    db.createCollection(TeamCollection.name)
    with(db.getCollection<UserFlags>(UserFlagsCollection.name)) {
        updateMany(
            UserFlags::githubId exists false,
            setValue(UserFlags::githubId, null),
        )
    }
}
