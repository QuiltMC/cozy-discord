/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.data

import org.quiltmc.community.cozy.modules.logs.Version

public data class Mod(
	val id: String,
	val version: Version,

	// Only present on Quilt Loader
	val path: String?,
	val hash: String?,
	val type: String?
)
