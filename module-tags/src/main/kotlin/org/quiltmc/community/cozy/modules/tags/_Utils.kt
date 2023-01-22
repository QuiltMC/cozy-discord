/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.tags

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.loadModule
import org.koin.dsl.bind
import org.quiltmc.community.cozy.modules.tags.config.SimpleTagsConfig
import org.quiltmc.community.cozy.modules.tags.config.TagsConfig
import org.quiltmc.community.cozy.modules.tags.data.TagsData
import org.quiltmc.community.cozy.modules.welcome.TagsExtension

public fun ExtensibleBotBuilder.ExtensionsBuilder.tags(config: TagsConfig, data: TagsData) {
	loadModule { single { config } bind TagsConfig::class }
	loadModule { single { data } bind TagsData::class }

	add { TagsExtension() }
}

public fun ExtensibleBotBuilder.ExtensionsBuilder.tags(data: TagsData, body: SimpleTagsConfig.Builder.() -> Unit) {
	tags(SimpleTagsConfig(body), data)
}

public fun String?.nullIfBlank(): String? =
	if (isNullOrBlank()) {
		null
	} else {
		this
	}
