/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import org.quiltmc.community.cozy.modules.ama.data.AmaConfig
import org.quiltmc.community.cozy.modules.ama.data.AmaData
import org.quiltmc.community.database.Collection
import org.quiltmc.community.database.Database
import org.quiltmc.community.database.entities.AmaEntity

class AmaConfigCollection : KordExKoinComponent, AmaData {
	private val database: Database by inject()
	private val col = database.mongo.getCollection<AmaEntity>(name)

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

	companion object : Collection("ama_config")
}
