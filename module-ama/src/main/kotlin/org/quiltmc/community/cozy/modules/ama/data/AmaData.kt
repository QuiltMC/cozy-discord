/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.ama.data

import dev.kord.common.entity.Snowflake

public interface AmaData {
	public suspend fun getConfig(guildId: Snowflake): AmaConfig?

	public suspend fun isButtonEnabled(guildId: Snowflake): Boolean?

	public suspend fun modifyButton(guildId: Snowflake, enabled: Boolean)

	public suspend fun setConfig(config: AmaConfig)
}
