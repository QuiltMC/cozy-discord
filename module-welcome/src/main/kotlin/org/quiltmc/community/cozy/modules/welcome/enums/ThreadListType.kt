/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class ThreadListType(public val humanReadable: String) {
    @SerialName("active")
    ACTIVE("Active"),

    @SerialName("newest")
    NEWEST("Recently Created")
}
