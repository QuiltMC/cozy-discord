/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.cozy.modules.ama.data.AmaConfig
import org.quiltmc.community.cozy.modules.ama.data.AmaEmbedConfig
import org.quiltmc.community.database.Entity

@Serializable
data class AmaEntity(
	override val _id: Snowflake,

	val guildId: Snowflake,
	val answerQueueChannel: Snowflake,
	val liveChatChannel: Snowflake,
	val buttonChannel: Snowflake,
	val approvalQueueChannel: Snowflake?,
	val flaggedQuestionChannel: Snowflake?,
	val embedConfig: AmaEmbedConfig,
	val buttonMessage: Snowflake,
	val buttonId: String,
	val enabled: Boolean
) : Entity<Snowflake> {
	fun toAmaConfig(): AmaConfig = AmaConfig(
		guildId = guildId,
		answerQueueChannel = answerQueueChannel,
		liveChatChannel = liveChatChannel,
		buttonChannel = buttonChannel,
		approvalQueueChannel = approvalQueueChannel,
		flaggedQuestionChannel = flaggedQuestionChannel,
		embedConfig = embedConfig,
		buttonMessage = buttonMessage,
		buttonId = buttonId,
		enabled = enabled
	)

	companion object {
		fun fromAmaConfig(config: AmaConfig): AmaEntity =
			AmaEntity(
				guildId = config.guildId,
				answerQueueChannel = config.answerQueueChannel,
				liveChatChannel = config.liveChatChannel,
				buttonChannel = config.buttonChannel,
				approvalQueueChannel = config.approvalQueueChannel,
				flaggedQuestionChannel = config.flaggedQuestionChannel,
				embedConfig = config.embedConfig,
				buttonMessage = config.buttonMessage,
				buttonId = config.buttonId,
				enabled = config.enabled,
				_id = config.guildId // TODO Is this correct i'm not sure
			)
	}
}
