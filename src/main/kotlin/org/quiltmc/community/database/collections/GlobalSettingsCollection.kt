/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import com.mongodb.client.result.UpdateResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.GlobalSettings

class GlobalSettingsCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<GlobalSettings>(name)

    suspend fun get() =
        col.findOne()

    suspend fun set(settings: GlobalSettings): UpdateResult? {
        var newSettings = settings
        val current = get()

        if (current != null) {
            newSettings = newSettings.copy(_id = current._id)
        }

        return col.save(newSettings)
    }

    companion object : Collection("global-settings")
}
