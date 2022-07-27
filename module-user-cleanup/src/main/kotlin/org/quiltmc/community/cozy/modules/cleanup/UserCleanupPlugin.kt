/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.cleanup

import com.kotlindiscord.kord.extensions.plugins.KordExPlugin
import com.kotlindiscord.kord.extensions.plugins.annotations.plugins.WiredPlugin
import org.pf4j.PluginWrapper

/**
 * Plugin containing the [UserCleanupExtension], which removes pending users after they've lurked for a while.
 */
@WiredPlugin(
	id = UserCleanupPlugin.id,
	version = "1.0.0-SNAPSHOT",

	author = "QuiltMC",
	description = "Automatic cleanup of lurking users that don't pass member screening",
	license = "Mozilla Public License 2.0"
)
public class UserCleanupPlugin(wrapper: PluginWrapper) : KordExPlugin(wrapper) {
	override suspend fun setup() {
// TODO: We can't really use the plugin system just yet since it doesn't currently support any configuration tooling
//        extension(::UserCleanupExtension)
	}

	public companion object {
		public const val id: String = "quiltmc-user-cleanup"
	}
}
