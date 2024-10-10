/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.rolesync

import dev.kordex.core.plugins.KordExPlugin

public class RoleSyncPlugin : KordExPlugin() {
	override suspend fun setup() {
// TODO: We can't really use the plugin system just yet since it doesn't currently support any configuration tooling
//        extension(::RoleSyncExtension)
	}

	public companion object {
		public const val id: String = "quiltmc-role-sync"
	}
}
