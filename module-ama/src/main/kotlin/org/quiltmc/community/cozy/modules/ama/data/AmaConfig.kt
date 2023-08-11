/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.ama.data

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
public data class AmaConfig(
	val guildId: Snowflake,
	val answerQueueChannel: Snowflake,
	val liveChatChannel: Snowflake,
	val buttonChannel: Snowflake,
	val approvalQueueChannel: Snowflake?,
	val flaggedQuestionChannel: Snowflake?,
	val embedConfig: AmaEmbedConfig,
	val buttonMessage: Snowflake,
	val buttonId: String,
	val enabled: Boolean,
)

@Serializable
public data class AmaEmbedConfig(
	val title: String,
	val description: String?,
	val imageUrl: String?
)
