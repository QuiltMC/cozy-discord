package org.quiltmc.community.api

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header

const val ROOT_DOMAIN = "https://phish.sinking.yachts"

const val ALL_PATH = "$ROOT_DOMAIN/all"
const val CHECK_PATH = "$ROOT_DOMAIN/check/%"
const val RECENT_PATH = "$ROOT_DOMAIN/recent/%"
const val SIZE_PATH = "$ROOT_DOMAIN/dbsize"

class PhishingApi {
    val client = HttpClient {
        install(JsonFeature)
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    suspend inline fun <reified T> get(url: String): T = client.get(url) {
        header("X-Identity", "QuiltMC, Cozy Discord bot")
    }

    suspend fun getAllDomains(): Set<String> =
        get(ALL_PATH)

    suspend fun checkDomain(domain: String): Boolean =
        get(CHECK_PATH.replace("%", domain))

    suspend fun getRecentDomains(seconds: Long): Set<String> =
        get(RECENT_PATH.replace("%", seconds.toString()))

    suspend fun getTotalDomains(): Long =
        get(SIZE_PATH)
}
