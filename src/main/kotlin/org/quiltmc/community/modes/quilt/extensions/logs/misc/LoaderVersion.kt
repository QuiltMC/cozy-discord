/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.misc

import kotlinx.serialization.Serializable

@Serializable
data class LoaderVersion(
    val loader: LoaderElement,
)

@Serializable
data class LoaderElement(
    val separator: String,
    val build: Int,
    val maven: String,
    val version: String
)
