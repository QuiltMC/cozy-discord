/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.logs.parsers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
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
            val loaderVersion = match.groups[2]!!.value.trim()

            @Suppress("TooGenericExceptionCaught")
            try {
                val currentVersion = getVersion(mcVersion)

                if (loaderVersion != currentVersion) {
                    messages.add(
                        "You appear to be using version `$loaderVersion` of Quilt Loader - please try updating to " +
                                "`$currentVersion`."
                    )
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

    suspend fun getVersion(mcVersion: String): String? {
        val url = "https://meta.quiltmc.org/v3/versions/loader/$mcVersion"
        val response: List<LoaderVersion> = client.get(url).body()
        val latest = response.maxByOrNull { it.loader.build }!!

        return latest.loader.version.trim()
    }
}
