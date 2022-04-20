/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.blocks

import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Suppress("UnnecessaryAbstractClass")
@Serializable
public abstract class Block {
    @Transient
    public lateinit var channel: GuildMessageChannel

    @Transient
    public lateinit var guild: Guild

    public abstract suspend fun create(builder: MessageCreateBuilder)
    public abstract suspend fun edit(builder: MessageModifyBuilder)
}
