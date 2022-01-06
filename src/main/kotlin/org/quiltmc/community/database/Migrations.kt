package org.quiltmc.community.database

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.quiltmc.community.database.collections.MetaCollection
import org.quiltmc.community.database.entities.Meta
import org.quiltmc.community.database.migrations.*

const val FILE_TEMPLATE = "migrations/v{VERSION}.bson"

object Migrations : KoinComponent {
    private val logger = KotlinLogging.logger { }

    val db: Database by inject()
    val metaColl: MetaCollection by inject()

    suspend fun migrate() {
        var meta = metaColl.get()

        if (meta == null) {
            meta = Meta(0)

            metaColl.set(meta)
        }

        var currentVersion = meta.version

        logger.info { "Current database version: v$currentVersion" }

        while (true) {
            val nextVersion = currentVersion + 1

            @Suppress("TooGenericExceptionCaught")
            try {
                @Suppress("MagicNumber")
                when (nextVersion) {  // TODO: This should REALLY be annotation-based
                    1 -> ::v1
                    2 -> ::v2
                    3 -> ::v3
                    4 -> ::v4
                    5 -> ::v5
                    6 -> ::v6
                    7 -> ::v7
                    8 -> ::v8
                    9 -> ::v9
                    10 -> ::v10

                    else -> break
                }(db.mongo)

                logger.info { "Migrated database to v$nextVersion" }
            } catch (t: Throwable) {
                logger.error(t) { "Failed to migrate database to v$nextVersion" }

                throw t
            }

            currentVersion = nextVersion
        }

        if (currentVersion != meta.version) {
            meta = meta.copy(version = currentVersion)

            metaColl.set(meta)

            logger.info { "Finished database migrations." }
        }
    }
}
