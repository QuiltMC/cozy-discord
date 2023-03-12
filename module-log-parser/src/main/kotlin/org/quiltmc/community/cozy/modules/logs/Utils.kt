/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs

import com.unascribed.flexver.FlexVerComparator
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkType
import java.net.URL

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
