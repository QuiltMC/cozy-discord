/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DataClassShouldBeImmutable", "DataClassContainsFunctions")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.Category
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.enums.QuiltServerType
import org.quiltmc.community.getGuildIgnoring403

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class ServerSettings(
	override val _id: Snowflake,

	var commandPrefix: String? = "?",
	val moderatorRoles: MutableSet<Snowflake> = mutableSetOf(),
	var verificationRole: Snowflake? = null,

	var cozyLogChannel: Snowflake? = null,
	var filterLogChannel: Snowflake? = null,
	var messageLogCategory: Snowflake? = null,

	var quiltServerType: QuiltServerType? = null,
	var leaveServer: Boolean = false,
) : Entity<Snowflake> {
	suspend fun save() {
		val collection = getKoin().get<ServerSettingsCollection>()

		collection.set(this)
	}

	suspend fun getConfiguredLogChannel(): TopGuildMessageChannel? {
		cozyLogChannel ?: return null

		val kord = getKoin().get<Kord>()
		val guild = kord.getGuildIgnoring403(_id)

		return guild?.getChannelOfOrNull(cozyLogChannel!!)
	}

	suspend fun getConfiguredMessageLogCategory(): Category? {
		messageLogCategory ?: return null

		val kord = getKoin().get<Kord>()
		val guild = kord.getGuildIgnoring403(_id)

		return guild?.getChannelOfOrNull(messageLogCategory!!)
	}

	suspend fun apply(embedBuilder: EmbedBuilder, showQuiltSettings: Boolean) {
		val kord = getKoin().get<Kord>()
		val builder = StringBuilder()
		val guild = kord.getGuildIgnoring403(_id)

		if (guild != null) {
			builder.append("**Guild ID:** `${_id.value}`\n")
		}

		builder.append("**Command Prefix:** `$commandPrefix`\n\n")
		builder.append("**Cozy Logs:** ")

		if (cozyLogChannel != null) {
			builder.append("<#${cozyLogChannel!!.value}>")
		} else {
			builder.append(":x: Not configured")
		}

		if (showQuiltSettings) {
			builder.append("\n")
			builder.append("**Filter Logs:** ")

			if (filterLogChannel != null) {
				builder.append("<#${filterLogChannel!!.value}>")
			} else {
				builder.append(":x: Not configured")
			}
		}

		builder.append("\n")
		builder.append("**Message Logs:** ")

		if (messageLogCategory != null) {
			builder.append("<#${messageLogCategory!!.value}>")
		} else {
			builder.append(":x: Not configured")
		}

		builder.append("\n\n")

		if (showQuiltSettings) {
			builder.append("**Quilt Server Type:** ")

			if (quiltServerType != null) {
				builder.append(quiltServerType!!.readableName)
			} else {
				builder.append(":x: Not configured")
			}

			builder.append("\n")
		}

		builder.append("**Leave Server Automatically:** ")

		if (leaveServer) {
			builder.append("Yes")
		} else {
			builder.append("No")
		}

		builder.append("**Verification role:** ")

		if (verificationRole != null) {
			builder.append("<@&$verificationRole>")
		} else {
			builder.append("N/A")
		}

		builder.append("\n\n")
		builder.append("**__Moderator Roles__**\n")

		if (moderatorRoles.isNotEmpty()) {
			moderatorRoles.forEach {
				val role = guild?.getRoleOrNull(it)

				if (role != null) {
					builder.append("**»** **${role.name}** (`${it.value}`)\n")
				} else {
					builder.append("**»** `${it.value}`\n")
				}
			}
		} else {
			builder.append(":x: No roles configured")
		}

		with(embedBuilder) {
			color = DISCORD_BLURPLE
			description = builder.toString()
			title = "Settings"

			if (guild != null) {
				title += ": ${guild.name}"
			} else {
				title += " (${_id.value})"
			}
		}
	}
}
