/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.data

public open class Launcher(
	public val name: String,
	public val version: String? = null
) {
	public companion object {
		public const val ATLauncher: String = "ATLauncher"
		public const val MultiMC: String = "MultiMC"
		public const val Prism: String = "Prism"
		public const val PolyMC: String = "PolyMC"
		public const val Technic: String = "Technic"
		public const val TLauncher: String = "TLauncher"
	}
}
