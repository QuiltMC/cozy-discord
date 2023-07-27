/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.ama.data

import dev.kord.common.entity.Snowflake

public class MemoryAmaData : AmaData {
	private val ama: MutableMap<Snowflake, AmaConfig> = mutableMapOf()

	override suspend fun getConfig(guildId: Snowflake): AmaConfig? =
		ama[guildId]

	override suspend fun isButtonEnabled(guildId: Snowflake): Boolean? =
		ama[guildId]?.enabled

	override suspend fun modifyButton(guildId: Snowflake, enabled: Boolean) {
		val amaConfig = ama[guildId]

		ama[guildId] = AmaConfig(
			amaConfig!!.guildId,
			amaConfig.answerQueueChannel,
			amaConfig.liveChatChannel,
			amaConfig.buttonChannel,
			amaConfig.approvalQueueChannel,
			amaConfig.flaggedQuestionChannel,
			amaConfig.embedConfig,
			amaConfig.buttonMessage,
			amaConfig.buttonId,
			enabled
		)
	}

	override suspend fun setConfig(config: AmaConfig) {
		ama[config.guildId] = config
	}

	override suspend fun usePluralKitFronter(user: Snowflake): Boolean = false
}
