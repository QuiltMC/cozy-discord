/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class OwnedThread(
	override val _id: Snowflake,

	var owner: Snowflake,
	val guild: Snowflake,
	var preventArchiving: Boolean = false,
) : Entity<Snowflake>
