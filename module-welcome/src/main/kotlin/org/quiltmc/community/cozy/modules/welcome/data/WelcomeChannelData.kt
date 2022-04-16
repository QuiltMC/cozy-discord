/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.data

import dev.kord.common.entity.Snowflake

public interface WelcomeChannelData {
    public suspend fun getChannelURLs(): Map<Snowflake, String>
    public suspend fun getUrlForChannel(channelId: Snowflake): String?

    public suspend fun setUrlForChannel(channelId: Snowflake, url: String)
    public suspend fun removeChannel(channelId: Snowflake): String?
}
