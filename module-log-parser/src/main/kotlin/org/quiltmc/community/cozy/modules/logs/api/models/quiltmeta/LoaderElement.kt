/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.api.models.quiltmeta

import io.github.z4kn4fein.semver.Version
import kotlinx.serialization.Serializable

@Serializable
public data class LoaderElement(
	val version: Version
)
