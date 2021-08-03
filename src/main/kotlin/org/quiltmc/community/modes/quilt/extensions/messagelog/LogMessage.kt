package org.quiltmc.community.modes.quilt.extensions.messagelog

import dev.kord.core.entity.Guild
import dev.kord.rest.builder.message.create.PersistentMessageCreateBuilder

data class LogMessage(
    val guild: Guild,

    val messageBuilder: suspend PersistentMessageCreateBuilder.() -> Unit
)
