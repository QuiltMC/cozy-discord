/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber", "UnderscoresInNumericLiterals")

package org.quiltmc.community

import dev.kord.common.entity.Snowflake
import dev.kordex.core.DISCORD_BLURPLE
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.utils.env
import dev.kordex.core.utils.envOrNull

internal val DISCORD_TOKEN = env("TOKEN")
internal val GITHUB_TOKEN = envOrNull("GITHUB_TOKEN")

internal val MAIN_GUILD = Snowflake(
	envOrNull("MAIN_GUILD_ID")?.toULong()
		?: envOrNull("COMMUNITY_GUILD_ID")?.toULong()
		?: 817576132726620200U
)

internal val STAFF_CATEGORIES = envOrNull("STAFF_CATEGORIES")
	?.split(',')
	?.map { Snowflake(it.trim()) }
	?: listOf(
		// Community server
		Snowflake("834516250021330949"),  // Overall bot logs
		Snowflake("839495849116958780"),  // Cozy message logs
		Snowflake("817576134535282729"),  // Information
		Snowflake("1085857118000787496"), // Feeds

		// Toolchain server
		Snowflake("834517525206925312"),  // Overall bot logs
		Snowflake("839496251463958548"),  // Cozy message logs
		Snowflake("833875973366874112"),  // Information

		// Collab server
		Snowflake("905435206830399508"),  // Overall bot logs
		Snowflake("905219585249247283"),  // Feeds
		Snowflake("905217110559563807"),  // Information
		Snowflake("1038162952634765332"),  // Applications (Internal)
		Snowflake("1026121640972664942"),  // Managers category
	)

internal val STAFF_CHANNELS = envOrNull("STAFF_CHANNELS")
	?.split(',')
	?.map { Snowflake(it.trim()) }
	?: listOf(
		// Community server
		Snowflake("972487531096592394"),  // Forum post logs

		Snowflake("1047954546992881694"), // Managers channel
		Snowflake("1086238572887150602"), // Admins channel

		// Toolchain server
		// Snowflake(""),

		// Collab server
		// Snowflake(""),
	)

internal val MESSAGE_LOG_CATEGORIES = envOrNull("MESSAGE_LOG_CATEGORIES")
	?.split(',')
	?.map { Snowflake(it.trim()) } ?: listOf()

internal val COLOUR_BLURPLE = DISCORD_BLURPLE
internal val COLOUR_NEGATIVE = DISCORD_RED
internal val COLOUR_POSITIVE = DISCORD_GREEN

internal val COMMUNITY_MODERATOR_ROLE = envOrNull("COMMUNITY_MODERATOR_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(863767207716192306)

internal val TOOLCHAIN_MODERATOR_ROLE = envOrNull("TOOLCHAIN_MODERATOR_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(863767485609541632)

internal val COMMUNITY_MANAGER_ROLE = envOrNull("COMMUNITY_MANAGER_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(832332800551813141)

internal val TOOLCHAIN_MANAGER_ROLE = envOrNull("TOOLCHAIN_MANAGER_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(833877938000494602)

internal val COLLAB_MANAGER_ROLE = envOrNull("COLLAB_MANAGER_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(905218921605513276)

internal val COMMUNITY_COMMUNITY_TEAM_ROLE = envOrNull("COMMUNITY_COMMUNITY_TEAM_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(863710574650327100)

internal val TOOLCHAIN_COMMUNITY_TEAM_ROLE = envOrNull("TOOLCHAIN_COMMUNITY_TEAM_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(863765983890374656)

internal val COMMUNITY_DEVELOPER_CATEGORY = envOrNull("COMMUNITY_DEVELOPER_CATEGORY")
	?.let { Snowflake(it) }
	?: Snowflake(1102169914858541066)

internal val COMMUNITY_DEVELOPER_ROLE = envOrNull("COMMUNITY_DEVELOPER_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(972868531844710412)

internal val TOOLCHAIN_DEVELOPER_ROLE = envOrNull("TOOLCHAIN_DEVELOPER_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(849305976951537725)

internal val MODERATOR_ROLES: List<Snowflake> =
	(envOrNull("MODERATOR_ROLES") ?: envOrNull("COMMUNITY_MANAGEMENT_ROLES")) // For now, back compat
		?.split(',')
		?.map { Snowflake(it.trim()) }
		?: listOf(COMMUNITY_MODERATOR_ROLE, TOOLCHAIN_MODERATOR_ROLE, COLLAB_MANAGER_ROLE)

internal val MANAGER_ROLES: List<Snowflake> =
	(envOrNull("MANAGER_ROLES") ?: envOrNull("COMMUNITY_MANAGER_ROLES"))
		?.split(',')
		?.map { Snowflake(it.trim()) }
		?: listOf(COMMUNITY_MANAGER_ROLE, TOOLCHAIN_MANAGER_ROLE, COLLAB_MANAGER_ROLE)

internal val COMMUNITY_TEAM_ROLES: List<Snowflake> =
	envOrNull("COMMUNITY_TEAM_ROLES")
		?.split(',')
		?.map { Snowflake(it.trim()) }
		?: listOf(COMMUNITY_COMMUNITY_TEAM_ROLE, TOOLCHAIN_COMMUNITY_TEAM_ROLE, COLLAB_MANAGER_ROLE)

internal val MINECRAFT_UPDATE_PING_ROLE = envOrNull("MINECRAFT_UPDATE_PING_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(1003614007237816361)

internal val COLLAB_VERIFIED_ROLE = envOrNull("COLLAB_VERIFIED_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(905218875015188531)

internal val TOOLCHAIN_COLLAB_ROLE = envOrNull("TOOLCHAIN_COLLAB_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(864199536221093889)

internal val COMMUNITY_COLLAB_ROLE = envOrNull("COMMUNITY_COLLAB_ROLE")
	?.let { Snowflake(it) }
	?: Snowflake(864178877142925312)

internal val COMMUNITY_GUILD = Snowflake(
	envOrNull("COMMUNITY_GUILD_ID")?.toLong()
		?: 817576132726620200
)

internal val TOOLCHAIN_GUILD = Snowflake(
	envOrNull("TOOLCHAIN_GUILD_ID")?.toLong()
		?: 833872081585700874
)

internal val COLLAB_GUILD = Snowflake(
	envOrNull("COLLAB_GUILD_ID")?.toLong()
		?: 905216141650198530
)

internal val GUILDS = envOrNull("GUILDS")
	?.split(',')
	?.map { Snowflake(it.trim()) }
	?: listOf(COMMUNITY_GUILD, TOOLCHAIN_GUILD)  // Not collab intentionally

internal val SUGGESTION_CHANNEL = Snowflake(
	envOrNull("SUGGESTION_CHANNEL_ID")?.toLong()
		?: 832353359074689084
)

internal val GALLERY_CHANNEL = Snowflake(
	envOrNull("GALLERY_CHANNEL_ID")?.toLong()
		?: 832348385997619300
)

internal val COMMUNITY_RELEASE_CHANNELS = envOrNull("COMMUNITY_RELEASE_CHANNELS")
	?.split(',')
	?.map { Snowflake(it.trim()) }
	?: listOf()

internal val DEVLOG_CHANNEL = Snowflake(
	envOrNull("DEVLOG_CHANNEL")?.toLong()
		?: 908399987099045999
)

internal val DEVLOG_FORUM = Snowflake(
	envOrNull("DEVLOG_FORUM")?.toLong()
		?: 1103979333627936818
)
