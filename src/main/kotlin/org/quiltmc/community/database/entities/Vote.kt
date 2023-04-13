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
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.database.Entity
import org.quiltmc.community.modes.quilt.extensions.voting.VoteStatus
import org.quiltmc.community.modes.quilt.extensions.voting.VoteType
import java.util.*

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class Vote(
	override val _id: UUID,

	var title: String,
	var description: String,
	val voteType: VoteType,

	var message: Snowflake? = null,
	var duration: DateTimePeriod? = null,
	var endTime: Instant? = null,

	var status: VoteStatus = VoteStatus.Draft,

	// Users with conflicts of interest that must be excluded from the vote
	val conflictsOfInterest: MutableList<Snowflake> = mutableListOf(),

	// Roles for an election or demotion
	val targetRoles: MutableList<Snowflake> = mutableListOf(),

	// Users for an election or demotion
	val targetUsers: MutableList<Snowflake> = mutableListOf(),

	// Users manually excluded from the vote, with reasons
	val exclusions: MutableMap<Snowflake, String> = mutableMapOf(),

	val abstentions: MutableList<Snowflake> = mutableListOf(),
	val positive: MutableList<Snowflake> = mutableListOf(),
	val negative: MutableList<Snowflake> = mutableListOf(),
) : Entity<UUID>
