/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.types

import org.quiltmc.community.cozy.modules.logs.data.Order

public interface BaseLogHandler {
	public val identifier: String
	public val order: Order
}

public fun <T : BaseLogHandler, C : Collection<T>> C.sortOrdered(): List<T> =
	this.sortedBy { it.order.value }
