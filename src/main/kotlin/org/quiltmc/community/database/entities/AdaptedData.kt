/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:UseSerializers(UUIDSerializer::class)

@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.github.jershell.kbson.UUIDSerializer
import com.kotlindiscord.kord.extensions.storage.StorageType
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.database.Entity

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class AdaptedData(
	override val _id: String,
	val identifier: String,

	val type: StorageType? = null,

	val channel: Snowflake? = null,
	val guild: Snowflake? = null,
	val message: Snowflake? = null,
	val user: Snowflake? = null,

	var data: String
) : Entity<String>
