/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:UseSerializers(UUIDSerializer::class)

@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import com.github.jershell.kbson.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.database.Entity
import org.quiltmc.community.modes.quilt.extensions.filtering.FilterAction
import org.quiltmc.community.modes.quilt.extensions.filtering.MatchTarget
import org.quiltmc.community.modes.quilt.extensions.filtering.MatchType
import java.util.*

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class FilterEntry(
	override val _id: UUID,

	var action: FilterAction?,
	var pingStaff: Boolean = false,

	var match: String,
	var matchType: MatchType,
	var matchTarget: MatchTarget,

	var note: String? = null,
) : Entity<UUID>
