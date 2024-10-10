/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import dev.kord.common.entity.Snowflake
import dev.kordex.core.checks.types.CheckContextWithCache
import dev.kordex.core.koin.KordExKoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import org.quiltmc.community.cozy.modules.ama.data.AmaConfig
import org.quiltmc.community.cozy.modules.ama.data.AmaData
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.AmaEntity
import org.quiltmc.community.hasBaseModeratorRole

class AmaConfigCollection : KordExKoinComponent, AmaData {
	private val database: Database by inject()
	private val col = database.mongo.getCollection<AmaEntity>(name)
	private val userFlags: UserFlagsCollection by inject()

	override suspend fun getConfig(guildId: Snowflake): AmaConfig? =
		col.findOne(AmaConfig::guildId eq guildId)?.toAmaConfig()

	override suspend fun isButtonEnabled(guildId: Snowflake): Boolean? =
		col.findOne(AmaConfig::guildId eq guildId)?.enabled

	override suspend fun modifyButton(guildId: Snowflake, enabled: Boolean) {
		col.findOneAndUpdate(AmaConfig::guildId eq guildId, setValue(AmaConfig::enabled, enabled))
	}

	override suspend fun setConfig(config: AmaConfig) {
		col.save(AmaEntity.fromAmaConfig(config))
	}

	override suspend fun usePluralKitFronter(user: Snowflake): Boolean =
		userFlags.get(user)?.usePKFronter ?: false

	override suspend fun CheckContextWithCache<*>.managementChecks() =
		hasBaseModeratorRole(includeCommunityManagers = true)

	companion object : Collection("ama_config")
}
