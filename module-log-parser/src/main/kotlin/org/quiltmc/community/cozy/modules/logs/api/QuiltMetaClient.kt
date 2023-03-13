/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.api

import io.github.z4kn4fein.semver.Version
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.quiltmc.community.cozy.modules.logs.api.models.quiltmeta.LoaderElement
import kotlin.time.Duration.Companion.minutes

private const val API_VERSION = 3
private const val BASE_URL = "https://meta.quiltmc.org/v$API_VERSION"
private const val LOADER_VERSIONS = "$BASE_URL/versions/loader"

public class QuiltMetaClient {
	private val client = HttpClient {
		install(ContentNegotiation) {
			json(
				Json { ignoreUnknownKeys = true },
				ContentType.Any
			)
		}

		defaultRequest {
			header("User-Agent", "Cozy Log Parser Module")
		}

		expectSuccess = true
	}

	private var lastRequest: Instant = Instant.DISTANT_PAST
	private var cache: List<Version> = listOf()

	public suspend fun getLoaderVersions(forceRefresh: Boolean = false): List<Version> {
		val now = Clock.System.now()

		if (forceRefresh || now - lastRequest > 10.minutes) {
			val versions: List<LoaderElement> = client.get(LOADER_VERSIONS).body()

			cache = versions.map { it.version }
			lastRequest = now
		}

		return cache
	}
}
