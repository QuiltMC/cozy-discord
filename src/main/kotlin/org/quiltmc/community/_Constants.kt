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

internal val COMMUNITY_MODERATOR_ROLE = env("COMMUNITY_MODERATOR_ROLE")?.let { Snowflake(it) }
    ?: Snowflake(863767207716192306)

internal val TOOLCHAIN_MODERATOR_ROLE = env("COMMUNITY_MODERATOR_ROLE")?.let { Snowflake(it) }
    ?: Snowflake(863767485609541632)

internal val MODERATOR_ROLES: List<Snowflake> =
    (env("MODERATOR_ROLES") ?: env("COMMUNITY_MANAGEMENT_ROLES")) // For now, back compat
        ?.split(',')
        ?.map { Snowflake(it.trim()) }
        ?: listOf(COMMUNITY_MODERATOR_ROLE, TOOLCHAIN_MODERATOR_ROLE)

internal val COMMUNITY_GUILD = Snowflake(
    env("COMMUNITY_GUILD_ID")?.toLong() ?: 817576132726620200
)

internal val TOOLCHAIN_GUILD = Snowflake(
    env("TOOLCHAIN_GUILD_ID")?.toLong() ?: 833872081585700874
)

internal val SUGGESTION_CHANNEL = Snowflake(
    env("SUGGESTION_CHANNEL_ID")?.toLong() ?: 832353359074689084
)

internal val SUGGESTION_LOG_CHANNEL = Snowflake(
    env("SUGGESTION_LOG_CHANNEL_ID")?.toLong() ?: 858693117645422622
)
