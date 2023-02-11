/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs

import com.kotlindiscord.kord.extensions.plugins.KordExPlugin
import com.kotlindiscord.kord.extensions.plugins.annotations.plugins.WiredPlugin
import org.pf4j.PluginWrapper

@WiredPlugin(
	"quiltmc-log-parser",
	"0.0.1",
	"QuiltMC",
	"Automatic log parsing extension, for people using Quilt or other mod-loaders",
	"MPL"
)
public class LogParserPlugin(wrapper: PluginWrapper) : KordExPlugin(wrapper) {
	public val name: String = "quiltmc-log-parser"

	override suspend fun setup() {
		extension(::LogParserExtension)
	}
}
