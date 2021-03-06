/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.api.pluralkit

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PKMessage(
    val timestamp: Instant,
    val id: Snowflake,
    val original: Snowflake,
    val sender: Snowflake,
    val channel: Snowflake,

    val system: PKSystem,
    val member: PKMember,
)
