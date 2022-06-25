/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("UNCHECKED_CAST")
@file:OptIn(InternalSerializationApi::class)

package org.quiltmc.community.database.storage

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.kotlindiscord.kord.extensions.storage.Data
import com.kotlindiscord.kord.extensions.storage.DataAdapter
import com.kotlindiscord.kord.extensions.storage.StorageUnit
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.AdaptedData

class MongoDBDataAdapter : DataAdapter<String>(), KordExKoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<AdaptedData>(name)

    private fun StorageUnit<*>.getIdentifier(): String =
        buildString {
            append("${storageType.type}/")

            if (guild != null) append("guild-$guild/")
            if (channel != null) append("channel-$channel/")
            if (user != null) append("user-$user/")
            if (message != null) append("message-$message/")

            append(identifier)
        }

    override suspend fun <R : Data> delete(unit: StorageUnit<R>): Boolean {
        removeFromCache(unit)

        val result = col.deleteOne(AdaptedData::_id eq unit.getIdentifier())

        return result.deletedCount > 0
    }

    override suspend fun <R : Data> get(unit: StorageUnit<R>): R? {
        val dataId = unitCache[unit]

        if (dataId != null) {
            val data = dataCache[dataId]

            if (data != null) {
                return data as R
            }
        }

        return reload(unit)
    }

    override suspend fun <R : Data> reload(unit: StorageUnit<R>): R? {
        val dataId = unit.getIdentifier()
        val result = col.findOne(AdaptedData::_id eq dataId)?.data

        if (result != null) {
            dataCache[dataId] = Json.decodeFromString(unit.dataType.serializer(), result)
            unitCache[unit] = dataId
        }

        return dataCache[dataId] as R?
    }

    override suspend fun <R : Data> save(unit: StorageUnit<R>): R? {
        val data = get(unit) ?: return null

        col.save(
            AdaptedData(
                unit.getIdentifier(),
                Json.encodeToString(unit.dataType.serializer(), data)
            )
        )

        return data
    }

    override suspend fun <R : Data> save(unit: StorageUnit<R>, data: R): R {
        val dataId = unit.getIdentifier()

        dataCache[dataId] = data
        unitCache[unit] = dataId

        col.save(
            AdaptedData(
                unit.getIdentifier(),
                Json.encodeToString(unit.dataType.serializer(), data)
            )
        )

        return data
    }

    companion object : Collection("adapted_data")
}
