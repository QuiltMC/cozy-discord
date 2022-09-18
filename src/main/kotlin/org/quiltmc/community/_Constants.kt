/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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

internal val COMMUNITY_MANAGER_ROLE = envOrNull("COMMUNITY_MODERATOR_ROLE")?.let { Snowflake(it) }
	?: Snowflake(832332800551813141)

internal val TOOLCHAIN_MANAGER_ROLE = envOrNull("TOOLCHAIN_MODERATOR_ROLE")?.let { Snowflake(it) }
	?: Snowflake(833877938000494602)

internal val COMMUNITY_DEVELOPER_ROLE = envOrNull("COMMUNITY_DEVELOPER_ROLE")?.let { Snowflake(it) }
	?: Snowflake(972868531844710412)

internal val TOOLCHAIN_DEVELOPER_ROLE = envOrNull("TOOLCHAIN_DEVELOPER_ROLE")?.let { Snowflake(it) }
	?: Snowflake(849305976951537725)

internal val MODERATOR_ROLES: List<Snowflake> =
	(envOrNull("MODERATOR_ROLES") ?: envOrNull("COMMUNITY_MANAGEMENT_ROLES")) // For now, back compat
		?.split(',')
		?.map { Snowflake(it.trim()) }
		?: listOf(COMMUNITY_MODERATOR_ROLE, TOOLCHAIN_MODERATOR_ROLE)

internal val MANAGER_ROLES: List<Snowflake> =
	(envOrNull("MANAGER_ROLES") ?: envOrNull("COMMUNITY_MANAGER_ROLES"))
		?.split(',')
		?.map { Snowflake(it.trim()) }
		?: listOf(COMMUNITY_MANAGER_ROLE, TOOLCHAIN_MANAGER_ROLE)

internal val MINECRAFT_UPDATE_PING_ROLE = envOrNull("MINECRAFT_UPDATE_PING_ROLE")?.let { Snowflake(it) }
	?: Snowflake(1003614007237816361)

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

internal val GALLERY_CHANNEL = Snowflake(
	envOrNull("GALLERY_CHANNEL_ID")?.toLong() ?: 832348385997619300
)
