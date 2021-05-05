package org.quiltmc.community

import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake
import dev.kord.common.kColor
import java.awt.Color

internal val TOKEN = env("TOKEN") ?: error("Required environment variable 'TOKEN' is missing.")

internal val GUILDS = env("GUILDS")?.split(',')?.map { Snowflake(it.trim()) }
    ?: error("Required environment variable 'GUILDS' is missing.")

internal val MESSAGE_LOG_CATEGORIES = env("MESSAGE_LOG_CATEGORIES")?.split(',')
    ?.map { Snowflake(it.trim()) } ?: listOf()

internal val COLOUR_BLURPLE = Color.decode("#758ADA").kColor
internal val COLOUR_NEGATIVE = Color.decode("#f54058").kColor
internal val COLOUR_POSITIVE = Color.decode("#2ac48e").kColor
