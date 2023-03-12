/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")

package org.quiltmc.community.cozy.modules.logs.data

public open class Order(public val value: Int) {
	public object Earlier : Order(-200)
	public object Early : Order(-100)
	public object Default : Order(0)
	public object Late : Order(100)
	public object Later : Order(200)
}
