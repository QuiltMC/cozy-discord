/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.parsers

import io.github.z4kn4fein.semver.Version
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.quiltmc.community.modes.quilt.extensions.logs.misc.LoaderVersion

private val MATCH_REGEX =
	"Loading Minecraft ([\\d.a-z]+) with Quilt Loader (\\S+)"
		.toRegex(RegexOption.IGNORE_CASE)

class LoaderVersionParser : BaseLogParser {
	val client = HttpClient {
		install(ContentNegotiation) {
			json(
				kotlinx.serialization.json.Json { ignoreUnknownKeys = true },
				ContentType.Any
			)
		}

		expectSuccess = true
	}

	override suspend fun getMessages(logContent: String): List<String> {
		val messages: MutableList<String> = mutableListOf()
		val match = MATCH_REGEX.find(logContent)

		if (match != null) {
			val mcVersion = match.groups[1]!!.value.trim()
			val loaderVersion = Version.parse(match.groups[2]!!.value.trim())

			@Suppress("TooGenericExceptionCaught")
			try {
				val currentVersions = getVersions(mcVersion)

				val latestStable = currentVersions
					.filter { it.isPreRelease.not() }
					.maxByOrNull { it }
					?: Version.min

				val latestBeta = currentVersions
					.filter { it.isPreRelease }
					.maxByOrNull { it }
					?: Version.min

				if (latestStable > loaderVersion) {
					messages.add(
						"You appear to be using version `$loaderVersion` of Quilt Loader - please try updating to " +
								"`$latestStable`."
					)
				} else if (loaderVersion.isPreRelease) {
					val suggestedVersion = maxOf(loaderVersion, latestStable, latestBeta)

					if (suggestedVersion > loaderVersion) {
						messages.add(
							"You appear to be using version `$loaderVersion` of Quilt Loader - please try updating " +
									"to `$suggestedVersion`."
						)
					}
				}
			} catch (e: Exception) {
				messages.add(
					"You appear to be using a version of Minecraft (`$mcVersion`) that Quilt Loader doesn't " +
							"currently support."
				)
			}
		}

		return messages
	}

	suspend fun getVersions(mcVersion: String): List<Version> {
		val url = "https://meta.quiltmc.org/v3/versions/loader/$mcVersion"
		val response: List<LoaderVersion> = client.get(url).body()
		return response.map { it.loader.version }
	}
}
