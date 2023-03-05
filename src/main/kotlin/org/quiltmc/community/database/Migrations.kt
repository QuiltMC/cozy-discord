/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.database.collections.MetaCollection
import org.quiltmc.community.database.entities.Meta
import org.quiltmc.community.database.migrations.*

const val FILE_TEMPLATE = "migrations/v{VERSION}.bson"

object Migrations : KordExKoinComponent {
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
				when (nextVersion) {  // TODO: This should **REEEEEEEEEAAALLLLLLLLLLLLLLLLLLLLLLLY** be annotation-based
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
					11 -> ::v11
					12 -> ::v12
					13 -> ::v13
					14 -> ::v14
					15 -> ::v15
					16 -> ::v16
					17 -> ::v17
					18 -> ::v18
					19 -> ::v19
					20 -> ::v20

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
