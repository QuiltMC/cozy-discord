/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:UseSerializers(UUIDSerializer::class)

@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.github.jershell.kbson.UUIDSerializer
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.database.Entity
import org.quiltmc.community.modes.quilt.extensions.voting.VoteStatus
import java.util.*

// Voter groups are for linking alt accounts together, so a user can't vote twice accidentally
@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class VoterGroup(
	override val _id: String,  // The group name, must be unique

	val accounts: MutableList<Snowflake> = mutableListOf(),
) : Entity<String>
