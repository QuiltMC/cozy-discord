/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.loadModule
import com.unascribed.flexver.FlexVerComparator
import org.koin.dsl.bind
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkType
import org.quiltmc.community.cozy.modules.logs.config.LogParserConfig
import org.quiltmc.community.cozy.modules.logs.config.SimpleLogParserConfig
import java.net.URL

public inline fun ExtensibleBotBuilder.ExtensionsBuilder.extLogParser(
	builder: (SimpleLogParserConfig.Builder).() -> Unit
) {
	val config = SimpleLogParserConfig(builder)

	loadModule { single { config } bind LogParserConfig::class }

	add(::LogParserExtension)
}

public fun ExtensibleBotBuilder.ExtensionsBuilder.extLogParser(config: LogParserConfig) {
	loadModule { single { config } bind LogParserConfig::class }

	add(::LogParserExtension)
}

public val linkExtractor: LinkExtractor = LinkExtractor.builder()
	.linkTypes(setOf(LinkType.URL))
	.build()

public fun String.parseUrls(): List<URL> =
	linkExtractor.extractLinks(this).map {
		URL(this.substring(it.beginIndex, it.endIndex))
	}

public fun String.versionCompare(other: String): Int =
	FlexVerComparator.compare(this, other)

@Suppress("SpreadOperator")
public fun String.endsWithExtensions(vararg extensions: String): Boolean =
	endsWithAny(*extensions.map { ".$it" }.toTypedArray())

public fun String.endsWithAny(vararg suffixes: String): Boolean =
	suffixes.any {
		this.endsWith(it)
	}

public fun String.startsWithAny(vararg prefixes: String): Boolean =
	prefixes.any {
		this.startsWith(it)
	}

public fun String.startsWithDomains(vararg domains: String): Boolean =
	domains.any {
		this.contains(Regex("^https?://$it"))
	}
