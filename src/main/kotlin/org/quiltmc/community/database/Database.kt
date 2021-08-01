package org.quiltmc.community.database

import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class Database(connectionString: String) {
    private val client = KMongo.createClient(connectionString).coroutine
    val mongo = client.getDatabase("cozy")

    suspend fun migrate() {
        Migrations.migrate()
    }
}
