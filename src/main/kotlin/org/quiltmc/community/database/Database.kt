/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import org.bson.UuidRepresentation
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class Database(connectionString: String) {
    private val clientSettings = MongoClientSettings
        .builder()
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .applyConnectionString(ConnectionString(connectionString))
        .build()

    private val client = KMongo.createClient(clientSettings).coroutine
    val mongo = client.getDatabase("cozy")

    suspend fun migrate() {
        Migrations.migrate()
    }
}
