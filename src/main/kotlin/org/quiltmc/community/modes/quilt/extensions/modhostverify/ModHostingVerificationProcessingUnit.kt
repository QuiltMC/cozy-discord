/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.modhostverify

import dev.kord.core.entity.Message
import dev.kord.core.entity.User

data class ModHostingVerificationProcessingUnit(
	val author: User,
	val message: Message,
	val missingFiles: MutableList<Project>,
	val remainingAttempts: Int
)
