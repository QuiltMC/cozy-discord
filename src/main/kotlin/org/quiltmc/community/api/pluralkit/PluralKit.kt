package org.quiltmc.community.api.pluralkit

import dev.kord.common.entity.Snowflake
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import mu.KotlinLogging

internal const val PK_BASE_URL = "https://api.pluralkit.me/v2"
internal const val MESSAGE_URL = "$PK_BASE_URL/messages/{id}"

class PluralKit {
    private val logger = KotlinLogging.logger { }

    private val client = HttpClient {
        install(JsonFeature) {
            this.serializer = KotlinxSerializer(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    suspend fun getMessage(id: Snowflake) =
        getMessage(id.toString())

    suspend fun getMessage(id: String): PKMessage {
        val url = MESSAGE_URL.replace("id" to id)

        try {
            val result: PKMessage = client.get(url)

            logger.info { "/messages/$id -> 200" }

            return result
        } catch (e: ClientRequestException) {
            logger.info { "/messages/$id -> ${e.response.status}" }

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
