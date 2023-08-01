/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.data

import dev.kord.core.entity.channel.Channel
import dev.kord.rest.builder.message.EmbedBuilder
import org.quiltmc.community.cozy.modules.logs.Version
import java.net.URL

public typealias Logs = List<Log>

public open class Log {
	public val environment: Environment = Environment()
	public var launcher: Launcher? = null
	public var launcherVersion: String? = null
	public var url: URL? = null

	public val extraEmbeds: MutableList<suspend (EmbedBuilder).() -> Unit> = mutableListOf()

	private val loaders: MutableMap<LoaderType, Version> = mutableMapOf()
	private val messages: MutableList<String> = mutableListOf()
	private val mods: MutableMap<String, Mod> = mutableMapOf()

	public lateinit var content: String

	public lateinit var channel: Channel
		internal set

	public var minecraftVersion: Version? = null
	public var hasProblems: Boolean = false

	public var abortReason: String? = null
		private set

	public val aborted: Boolean get() =
		abortReason != null

	public var fromCommand: Boolean = false
		internal set

	public open fun embed(body: suspend (EmbedBuilder).() -> Unit) {
		extraEmbeds.add(body)
	}

	public open fun abort(message: String) {
		abortReason = message
	}

	public open fun getLoaders(): Map<LoaderType, Version> =
		loaders.toMap()

	public open fun getLoaderVersion(type: LoaderType): Version? =
		loaders[type]

	public open fun setLoaderVersion(type: LoaderType, version: Version) {
		loaders[type] = version
	}

	public open fun getMessages(): List<String> =
		messages.toList()

	public open fun addMessage(message: String): Boolean =
		messages.add(message)

	public open fun getMods(): Map<String, Mod> =
		mods.toMap()

	public open fun getMod(id: String): Mod? =
		mods[id]

	public open fun addMod(mod: Mod) {
		mods[mod.id] = mod
	}
}
