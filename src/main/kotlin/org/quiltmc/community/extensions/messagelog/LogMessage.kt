package org.quiltmc.community.extensions.messagelog

import dev.kord.core.entity.Guild
import dev.kord.rest.builder.message.MessageCreateBuilder

data class LogMessage(
    val guild: Guild,

    val messageBuilder: suspend MessageCreateBuilder.() -> Unit
)
