package org.quiltmc.community.modes.quilt.extensions.logs.parsers

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.quiltmc.community.modes.quilt.extensions.logs.misc.ModrinthVersion

private val MATCH_REGEX = "- quilted_fabric_api ([^\\n]+)".toRegex(RegexOption.IGNORE_CASE)
private const val MODRINTH_URL = "https://api.modrinth.com/v2/project/qsl/version"

class QSLVersionParser : BaseLogParser {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true },
                ContentType.Any
            )
        }

        expectSuccess = true
    }

    override suspend fun getMessages(logContent: String): List<String> {
        val messages: MutableList<String> = mutableListOf()
        val match = MATCH_REGEX.find(logContent)

        if (match != null) {
            val providedVersion = match.groups[1]!!.value.trim()
            val (version, url) = getVersion() ?: return emptyList()

            if (!providedVersion.equals(version, true)) {
                messages.add(
                    "You appear to be using version `$providedVersion` of QSL - please try updating to " +
                            "[version `$version`]($url)."
                )
            }
        }

        return messages
    }

    suspend fun getVersion(): Pair<String, String>? {
        val response: List<ModrinthVersion> = client.get(MODRINTH_URL).body()

        val latest = response
            .maxByOrNull { it.datePublished }
            ?: return null

        return latest.versionNumber to latest.files.first { it.primary }.url
    }
}
