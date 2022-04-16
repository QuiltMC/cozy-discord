/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.blocks

import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import kotlinx.serialization.Serializable

@Suppress("UnnecessaryAbstractClass")
@Serializable
public abstract class Block {
    public abstract suspend fun create(builder: MessageCreateBuilder)
    public abstract suspend fun edit(builder: MessageModifyBuilder)
}
