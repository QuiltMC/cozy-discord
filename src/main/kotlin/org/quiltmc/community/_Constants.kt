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
internal val GH_ORG_SLUG = envOrNull("GH_ORG_SLUG") ?: "quiltmc"
internal val GH_ORG_ID = envOrNull("GH_ORG_ID") ?: "78571508"
internal val GH_COZY_ID = envOrNull("GH_COZY_ID") ?: "86573733"

internal val MAIN_GUILD = Snowflake(
    envOrNull("MAIN_GUILD_ID")?.toULong()
        ?: envOrNull("COMMUNITY_GUILD_ID")?.toULong()
        ?: 817576132726620200U
)

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

internal val COMMUNITY_ADMIN_ROLE = envOrNull("COMMUNITY_ADMIN_ROLE")?.let { Snowflake(it) }
    ?: Snowflake(236548787739295744)

internal val TOOLCHAIN_ADMIN_ROLE = envOrNull("TOOLCHAIN_ADMIN_ROLE")?.let { Snowflake(it) }
    ?: Snowflake(833877395046924349)

// does this really need an environment variable?
internal val ADMIN_ROLES: List<Snowflake> = listOf(COMMUNITY_ADMIN_ROLE, TOOLCHAIN_ADMIN_ROLE)

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

internal val GITHUB_LOG_CHANNEL = Snowflake(
    envOrNull("GITHUB_LOG_CHANNEL_ID")?.toLong() ?: 906285091481849876
)
