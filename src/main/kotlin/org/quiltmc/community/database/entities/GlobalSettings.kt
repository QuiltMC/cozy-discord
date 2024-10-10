/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:UseSerializers(UUIDSerializer::class)

@file:Suppress("DataClassShouldBeImmutable", "DataClassContainsFunctions")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.github.jershell.kbson.UUIDSerializer
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kordex.core.DISCORD_BLURPLE
import dev.kordex.core.utils.getKoin
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.database.Entity
import org.quiltmc.community.database.collections.GlobalSettingsCollection
import org.quiltmc.community.getGuildIgnoring403
import java.util.*

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class GlobalSettings(
	override val _id: UUID = UUID.randomUUID(),

	var appealsInvite: String? = null,
	var githubToken: String? = null,

	val quiltGuilds: MutableSet<Snowflake> = mutableSetOf(
		Snowflake(817576132726620200U),  // Community
//		Snowflake(833872081585700874U),  // Toolchain
	),

	var suggestionChannel: Snowflake? = Snowflake(832353359074689084U),
	var githubLogChannel: Snowflake? = Snowflake(906285091481849876U),
) : Entity<UUID> {
	suspend fun save() {
		val collection = getKoin().get<GlobalSettingsCollection>()

		collection.set(this)
	}

	suspend fun apply(embedBuilder: EmbedBuilder) {
		val kord = getKoin().get<Kord>()
		val builder = StringBuilder()

		builder.append("**Appeals Invite:** ")

		if (appealsInvite != null) {
			builder.append("https://discord.gg/$appealsInvite")
		} else {
			builder.append(":x: Not configured")
		}

		builder.append("\n")
		builder.append("**GitHub Token:** ")

		if (githubToken != null) {
			builder.append("Configured")
		} else {
			builder.append(":x: Not configured")
		}

		builder.append("\n\n")
		builder.append("**Suggestions Channel:** ")

		if (suggestionChannel != null) {
			builder.append("<#${suggestionChannel!!.value}>")
		} else {
			builder.append(":x: Not configured")
		}

		builder.append("\n")
		builder.append("**/github Log Channel:** ")

		if (githubLogChannel != null) {
			builder.append("<#${githubLogChannel!!.value}>")
		} else {
			builder.append(":x: Not configured")
		}

		builder.append("\n\n")
		builder.append("__**Quilt Servers**__\n")

		if (quiltGuilds.isNotEmpty()) {
			quiltGuilds.forEach {
				val guild = kord.getGuildIgnoring403(it)

				if (guild == null) {
					builder.append("**»** `${it.value}`\n")
				} else {
					builder.append("**»** ${guild.name} (`${it.value}`)\n")
				}
			}
		} else {
			builder.append(":x: No servers configured")
		}

		with(embedBuilder) {
			title = "Global Settings"
			color = DISCORD_BLURPLE

			description = builder.toString()
		}
	}
}
