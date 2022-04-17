/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.api.pluralkit

import dev.kord.common.entity.Snowflake
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import mu.KotlinLogging

internal const val PK_BASE_URL = "https://api.pluralkit.me/v2"
internal const val MESSAGE_URL = "$PK_BASE_URL/messages/{id}"

class PluralKit {
    private val logger = KotlinLogging.logger { }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true },
                ContentType.Any
            )
        }

        expectSuccess = true
    }

    suspend fun getMessage(id: Snowflake) =
        getMessage(id.toString())

    @Suppress("MagicNumber")
    suspend fun getMessage(id: String): PKMessage {
        val url = MESSAGE_URL.replace("id" to id)

        try {
            val result: PKMessage = client.get(url).body()

            logger.debug { "/messages/$id -> 200" }

            return result
        } catch (e: ClientRequestException) {
            if (e.response.status.value in 400 until 499) {
                logger.error(e) { "/messages/$id -> ${e.response.status}" }
            }

            throw e
        }
    }

    suspend fun getMessageOrNull(id: Snowflake) =
        getMessageOrNull(id.toString())

    suspend fun getMessageOrNull(id: String): PKMessage? {
        try {
            return getMessage(id)
        } catch (e: ClientRequestException) {
            if (e.response.status.value != HttpStatusCode.NotFound.value) {
                throw e
            }
        }

        return null
    }

    private fun String.replace(vararg pairs: Pair<String, Any>): String {
        var result = this

        pairs.forEach { (k, v) ->
            result = result.replace("{$k}", v.toString())
        }

        return result
    }
}
