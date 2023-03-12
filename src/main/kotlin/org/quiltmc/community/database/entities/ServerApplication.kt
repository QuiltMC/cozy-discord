/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:UseSerializers(UUIDSerializer::class)

@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.github.jershell.kbson.UUIDSerializer
import com.kotlindiscord.kord.extensions.events.extra.models.ApplicationStatus
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.database.Entity
import java.util.*

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class ServerApplication(
	override val _id: Snowflake,

	var status: ApplicationStatus?,
	var threadId: Snowflake? = null,

	val userId: Snowflake,
	val guildId: Snowflake,
	val messageId: Snowflake,

	val messageLink: String? = null,
	var rejectionReason: String? = null,
	var actionedAt: Instant? = null,
) : Entity<Snowflake>
