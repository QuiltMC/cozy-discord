/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.types

import org.quiltmc.community.cozy.modules.logs.data.Order

public interface Ordered {
	public val order: Order
}

public fun <T : Ordered, C : Collection<T>> C.sortOrdered(): List<T> =
	this.sortedBy { it.order.value }
