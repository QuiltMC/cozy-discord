@file:Suppress("MagicNumber", "UnderscoresInNumericLiterals")

package org.quiltmc.community

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull
import dev.kord.common.entity.Snowflake

internal val DISCORD_TOKEN = env("TOKEN")
internal val GITHUB_TOKEN = envOrNull("GITHUB_TOKEN")

internal val MESSAGE_LOG_CATEGORIES = envOrNull("MESSAGE_LOG_CATEGORIES")?.split(',')
    ?.map { Snowflake(it.trim()) } ?: listOf()

internal val COLOUR_BLURPLE = DISCORD_BLURPLE
internal val COLOUR_NEGATIVE = DISCORD_RED
internal val COLOUR_POSITIVE = DISCORD_GREEN

internal val COMMUNITY_MODERATOR_ROLE = envOrNull("COMMUNITY_MODERATOR_ROLE")?.let { Snowflake(it) }
    ?: Snowflake(863767207716192306)

internal val TOOLCHAIN_MODERATOR_ROLE = envOrNull("TOOLCHAIN_MODERATOR_ROLE")?.let { Snowflake(it) }
    ?: Snowflake(863767485609541632)

internal val MODERATOR_ROLES: List<Snowflake> =
    (envOrNull("MODERATOR_ROLES") ?: envOrNull("COMMUNITY_MANAGEMENT_ROLES")) // For now, back compat
        ?.split(',')
        ?.map { Snowflake(it.trim()) }
        ?: listOf(COMMUNITY_MODERATOR_ROLE, TOOLCHAIN_MODERATOR_ROLE)

internal val COMMUNITY_GUILD = Snowflake(
    envOrNull("COMMUNITY_GUILD_ID")?.toLong() ?: 817576132726620200
)

internal val TOOLCHAIN_GUILD = Snowflake(
    envOrNull("TOOLCHAIN_GUILD_ID")?.toLong() ?: 833872081585700874
)

internal val GUILDS = envOrNull("GUILDS")?.split(',')?.map { Snowflake(it.trim()) }
    ?: listOf(COMMUNITY_GUILD, TOOLCHAIN_GUILD)

internal val SUGGESTION_CHANNEL = Snowflake(
    envOrNull("SUGGESTION_CHANNEL_ID")?.toLong() ?: 832353359074689084
)

internal val SUGGESTION_LOG_CHANNEL = Snowflake(
    envOrNull("SUGGESTION_LOG_CHANNEL_ID")?.toLong() ?: 858693117645422622
)
