@file:Suppress("MagicNumber", "UnderscoresInNumericLiterals")

package org.quiltmc.community

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake

internal val TOKEN = env("TOKEN") ?: error("Required environment variable 'TOKEN' is missing.")

internal val GUILDS = env("GUILDS")?.split(',')?.map { Snowflake(it.trim()) }
    ?: listOf(Snowflake(817576132726620200), Snowflake(833872081585700874))

internal val MESSAGE_LOG_CATEGORIES = env("MESSAGE_LOG_CATEGORIES")?.split(',')
    ?.map { Snowflake(it.trim()) } ?: listOf()

internal val COLOUR_BLURPLE = DISCORD_BLURPLE
internal val COLOUR_NEGATIVE = DISCORD_RED
internal val COLOUR_POSITIVE = DISCORD_GREEN

internal val COMMUNITY_MANAGEMENT_ROLES: List<Snowflake> =
    env("COMMUNITY_MANAGEMENT_ROLES")?.split(',')?.map { Snowflake(it.trim()) }
        ?: listOf(Snowflake(833808862628544560), Snowflake(832332800551813141))

internal val COMMUNITY_GUILD = Snowflake(
    env("COMMUNITY_GUILD_ID")?.toLong() ?: 817576132726620200
)

internal val SUGGESTION_CHANNEL = Snowflake(
    env("SUGGESTION_CHANNEL_ID")?.toLong() ?: 832353359074689084
)

internal val SUGGESTION_LOG_CHANNEL = Snowflake(
    env("SUGGESTION_LOG_CHANNEL_ID")?.toLong() ?: 858693117645422622
)
