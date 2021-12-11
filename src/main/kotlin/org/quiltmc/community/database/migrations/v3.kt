package org.quiltmc.community.database.migrations

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.ReplaceOneModel
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.replaceOne
import org.litote.kmongo.replaceUpsert
import org.quiltmc.community.COMMUNITY_GUILD
import org.quiltmc.community.database.collections.OwnedThreadCollection
import org.quiltmc.community.database.collections.SuggestionsCollection
import org.quiltmc.community.database.entities.OwnedThread
import org.quiltmc.community.database.entities.Suggestion

suspend fun v3(db: CoroutineDatabase) {
    db.createCollection(OwnedThreadCollection.name)

    val suggestions = db.getCollection<Suggestion>(SuggestionsCollection.name)
    val ownedThreads = db.getCollection<OwnedThread>(OwnedThreadCollection.name)

    val documents = mutableListOf<ReplaceOneModel<OwnedThread>>()

    suggestions.find().consumeEach {
        if (it.thread != null) {
            documents.add(
                replaceOne(
                    OwnedThread::_id eq it.thread!!,

                    OwnedThread(
                        it.thread!!,
                        it.owner,
                        COMMUNITY_GUILD,
                        false
                    ),

                    replaceUpsert()
                )
            )
        }
    }

    if (documents.isNotEmpty()) {
        ownedThreads.bulkWrite(requests = documents, BulkWriteOptions().ordered(false))
    }
}
