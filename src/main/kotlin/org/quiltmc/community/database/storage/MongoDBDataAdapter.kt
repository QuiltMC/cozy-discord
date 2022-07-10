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
import org.bson.conversions.Bson
import org.koin.core.component.inject
import org.litote.kmongo.and
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.AdaptedData

class MongoDBDataAdapter : DataAdapter<String>(), KordExKoinComponent {
    private val database: Database by inject()
    private val collectionCache: MutableMap<String, CoroutineCollection<AdaptedData>> = mutableMapOf()

    private fun StorageUnit<*>.getIdentifier(): String =
        buildString {
            append("${storageType.type}/")

            if (guild != null) append("guild-$guild/")
            if (channel != null) append("channel-$channel/")
            if (user != null) append("user-$user/")
            if (message != null) append("message-$message/")

            append(identifier)
        }

    private suspend fun getCollection(namespace: String): CoroutineCollection<AdaptedData> {
        val collName = "data-$namespace"
        var coll = collectionCache[collName]

        if (coll == null) {
            coll = database.mongo.getCollection(collName)

            collectionCache[collName] = coll
        }

        return coll
    }

    private fun constructQuery(unit: StorageUnit<*>): Bson {
        var query = AdaptedData::_id eq unit.identifier

        query = and(query, AdaptedData::type eq unit.storageType)

        query = and(query, AdaptedData::channel eq unit.channel)
        query = and(query, AdaptedData::guild eq unit.guild)
        query = and(query, AdaptedData::message eq unit.message)
        query = and(query, AdaptedData::user eq unit.user)

        return query
    }

    override suspend fun <R : Data> delete(unit: StorageUnit<R>): Boolean {
        removeFromCache(unit)

        val result = getCollection(unit.namespace)
            .deleteOne(constructQuery(unit))

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
        val result = getCollection(unit.namespace)
            .findOne(constructQuery(unit))?.data

        if (result != null) {
            dataCache[dataId] = Json.decodeFromString(unit.dataType.serializer(), result)
            unitCache[unit] = dataId
        }

        return dataCache[dataId] as R?
    }

    override suspend fun <R : Data> save(unit: StorageUnit<R>): R? {
        val data = get(unit) ?: return null

        getCollection(unit.namespace).save(
            AdaptedData(
                _id = unit.getIdentifier(),

                identifier = unit.identifier,

                type = unit.storageType,

                channel = unit.channel,
                guild = unit.guild,
                message = unit.message,
                user = unit.user,

                data = Json.encodeToString(unit.dataType.serializer(), data)
            )
        )

        return data
    }

    override suspend fun <R : Data> save(unit: StorageUnit<R>, data: R): R {
        val dataId = unit.getIdentifier()

        dataCache[dataId] = data
        unitCache[unit] = dataId

        getCollection(unit.namespace).save(
            AdaptedData(
                _id = unit.getIdentifier(),

                identifier = unit.identifier,

                type = unit.storageType,

                channel = unit.channel,
                guild = unit.guild,
                message = unit.message,
                user = unit.user,

                data = Json.encodeToString(unit.dataType.serializer(), data)
            )
        )

        return data
    }
}
