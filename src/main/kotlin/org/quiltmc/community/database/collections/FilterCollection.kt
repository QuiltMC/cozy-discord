/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.FilterEntry
import java.util.*

class FilterCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<FilterEntry>(name)

    suspend fun get(id: UUID) =
        col.findOne(FilterEntry::_id eq id)

    suspend fun getAll() =
        col.find().toList()

    suspend fun set(filter: FilterEntry) =
        col.save(filter)

    suspend fun remove(filter: FilterEntry) =
        col.deleteOne(FilterEntry::_id eq filter._id)

    suspend fun remove(id: UUID) =
        col.deleteOne(FilterEntry::_id eq id)

    companion object : Collection("filters")
}
