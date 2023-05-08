/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("DataClassShouldBeImmutable", "DataClassContainsFunctions")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.database.Entity
import org.quiltmc.community.database.collections.LinkedMessagesCollection

@Serializable
data class LinkedMessages(
	override val _id: Snowflake,

	val targets: MutableList<Snowflake> = mutableListOf()
) : Entity<Snowflake> {
	suspend fun save() {
		val collection = getKoin().get<LinkedMessagesCollection>()

		collection.set(this)
	}
}