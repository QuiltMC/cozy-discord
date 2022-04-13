/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.tags

import com.kotlindiscord.kord.extensions.plugins.KordExPlugin
import com.kotlindiscord.kord.extensions.plugins.annotations.plugins.WiredPlugin
import org.pf4j.PluginWrapper

/**
 * Plugin containing the [UserCleanupExtension], which removes pending users after they've lurked for a while.
 */
@WiredPlugin(
    id = TagsPlugin.id,
    version = "1.0.0-SNAPSHOT",

    author = "QuiltMC",
    description = "Tags system, allowing for the addition and display of configurable text snippets.",
    license = "Mozilla Public License 2.0"
)
public class TagsPlugin(wrapper: PluginWrapper) : KordExPlugin(wrapper) {
    override suspend fun setup() {
// TODO: We can't really use the plugin system just yet since it doesn't currently support any configuration tooling
//        extension(::TagsExtension)
    }

    public companion object {
        public const val id: String = "quiltmc-tags"
    }
}
