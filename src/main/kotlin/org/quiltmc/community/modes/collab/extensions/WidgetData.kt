/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.collab.extensions

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WidgetData(
    val id: Snowflake,
    val name: String,

    @SerialName("instant_invite")
    val instantInvite: String? = null,

    @SerialName("presence_count")
    val presenceCount: Long,

    val channels: List<WidgetChannel>,
    val members: List<WidgetMember>,
)

@Serializable
data class WidgetMember(
    @SerialName("id")
    val order: Long,

    val username: String,
    val status: String,

    @SerialName("avatar_url")
    val avatarUrl: String,
)

@Serializable
data class WidgetChannel(
    val id: Snowflake,
    val name: String,
    val position: Long,
)
