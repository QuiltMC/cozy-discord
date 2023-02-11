/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs

import com.unascribed.flexver.FlexVerComparator

@JvmInline
public value class Version(
	public val string: String,
) {
	public operator fun compareTo(other: Version): Int =
		FlexVerComparator.compare(this.string, other.string)
}
