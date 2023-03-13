/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.api

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
import org.quiltmc.community.cozy.modules.logs.api.models.modrinth.ModrinthVersion
import kotlin.time.Duration.Companion.minutes

private const val API_VERSION = 2
private const val BASE_URL = "https://api.modrinth.com/v$API_VERSION"
private const val PROJECT_VERSION = "$BASE_URL/project/:project/version"

public class ModrinthClient {
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

	private val requestTimes: MutableMap<String, Instant> =
		mutableMapOf<String, Instant>()

	private val caches: MutableMap<String, List<ModrinthVersion>> = mutableMapOf()

	public suspend fun getProjectVersions(project: String, forceRefresh: Boolean = false): List<ModrinthVersion>? {
		val now = Clock.System.now()
		val lastRequest = requestTimes.getOrDefault(project, Instant.DISTANT_PAST)

		if (forceRefresh || now - lastRequest > 10.minutes) {
			val versions: List<ModrinthVersion> = try {
				client.get(
					PROJECT_VERSION.replace(":project", project)
				).body()
			} catch (e: ClientRequestException) {
				if (e.response.status == HttpStatusCode.NotFound) {
					return null
				}

				throw (e)
			}

			caches[project] = versions
			requestTimes[project] = now
		}

		return caches[project]
	}
}
