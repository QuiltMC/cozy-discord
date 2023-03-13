/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:UseSerializers(MatchRegexSerializer::class)

package org.quiltmc.community.cozy.modules.logs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.quiltmc.community.cozy.modules.logs.MatchRegexSerializer

@Serializable
public data class UrlTransformSplit(
	public val group: Int,
	public val string: String
)

@Serializable
public data class UrlTransform(
	public val match: Regex,
	public val output: String,

	public val split: UrlTransformSplit? = null
)

@Serializable
public enum class ScrapeType {
	@SerialName("first-element")
	FirstElement,

	@SerialName("hrefs")
	Hrefs
}

@Serializable
public data class Scrape(
	public val match: Regex,
	public val type: ScrapeType,
	public val selector: String,
)

@Serializable
public data class PastebinConfig(
	public val raw: List<Regex>,

	@SerialName("url-transform")
	public val urlTransform: List<UrlTransform>,
	public val scrape: List<Scrape>,
)
