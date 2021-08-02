package org.quiltmc.community.database.collections

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.Meta

class MetaCollection : KoinComponent {
    private val database: Database by inject()
    private val col = database.mongo.getCollection<Meta>(name)

    suspend fun get() =
        col.findOne()

    suspend fun set(meta: Meta) =
        col.save(meta)

    companion object : Collection("meta")
}
