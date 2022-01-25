/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.messagelog

import dev.kord.core.entity.Guild
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder

data class LogMessage(
    val guild: Guild,

    val messageBuilder: suspend UserMessageCreateBuilder.() -> Unit
)
