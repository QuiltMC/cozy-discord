package org.quiltmc.community.github

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.quiltmc.community.GH_ORG_ID
import org.quiltmc.community.GH_ORG_SLUG
import org.quiltmc.community.GITHUB_TOKEN

/// This is a tiny wrapper around the Github REST API, providing only
/// what Cozy specifically needs.
/// It

private val client by lazy {
    HttpClient(engineFactory = CIO, block = {
        defaultRequest {
            header("Authorization", "bearer $GITHUB_TOKEN")
            header("Accept", "application/vnd.github.v3+json")
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                encodeDefaults = false
                ignoreUnknownKeys = true
            })
        }
        expectSuccess = false
    })
}
private const val GH_URL: String = "https://api.github.com"
private val ORG_URL_ID: String = "$GH_URL/organizations/$GH_ORG_ID"
private val ORG_URL_SLUG: String = "$GH_URL/orgs/$GH_ORG_SLUG"

@Serializable
data class GhTeam(val name: String, val id: DatabaseId, val slug: String, val permission: String) {
    companion object {
        suspend fun get(slug: String?): GhTeam? {
            if (slug == null) {
                return null
            }

            return client.get<HttpResponse>(url(slug)).receiveOrNull()
        }

        suspend fun get(id: DatabaseId): GhTeam? {
            return client.get<HttpResponse>(url(id)).receiveOrNull()
        }

        suspend fun members(slug: String): List<GhUser> {
            return client.get(url(slug) + "/members")
        }

        suspend fun members(id: DatabaseId): List<GhUser> {
            return client.get(url(id) + "/members")
        }

        //region mutations
        suspend fun addMember(teamId: DatabaseId, userLogin: String): AddMemberResult {
            return addMember(url(teamId) + "/memberships/$userLogin")
        }

        suspend fun addMember(teamSlug: String, userLogin: String): AddMemberResult {
            return addMember(url(teamSlug) + "/memberships/$userLogin")
        }

        private suspend fun addMember(url: String): AddMemberResult {
            val response: HttpResponse = client.put(url)
            return if (response.status.isSuccess()) {
                AddMemberResult(
                    added = true,
                    pending = response.receive<String>().contains("pending"), // hack
                    response.status
                )
            } else {
                AddMemberResult(added = false, pending = false, response.status)
            }
        }

        suspend fun removeMember(teamId: DatabaseId, userLogin: String) {
            removeMember(url(teamId) + "/memberships/$userLogin")
        }

        suspend fun removeMember(teamSlug: String, userLogin: String) {
            removeMember(url(teamSlug) + "/memberships/$userLogin")
        }

        private suspend fun removeMember(url: String) {
            val response: HttpResponse = client.delete(url)
            if (!response.status.isSuccess()) {
                throw Exception(response.status.toString())
            }
        }
        //endregion

        private fun url(slug: String): String {
            return "$ORG_URL_SLUG/teams/$slug"
        }

        private fun url(id: DatabaseId): String {
            return "$ORG_URL_ID/team/$id"
        }

        data class AddMemberResult(val added: Boolean, val pending: Boolean, val statusCode: HttpStatusCode)
    }

    suspend fun addMember(userLogin: String): AddMemberResult {
        return addMember(this.id, userLogin)
    }

    suspend fun removeMember(userLogin: String) {
        return removeMember(this.id, userLogin)
    }

    suspend fun members(): List<GhUser> {
        return members(this.id)
    }
}

@Serializable
data class GhUser(val login: String, val id: DatabaseId) {
    companion object {
        suspend fun get(login: String?): GhUser? {
            if (login == null) {
                return null
            }

            return client.get<HttpResponse>("$GH_URL/users/$login").receiveOrNull()
        }

        suspend fun get(id: DatabaseId?): GhUser? {
            if (id == null) {
                return null
            }

            // Undocumented endpoint! what fun!
            return client.get<HttpResponse>("$GH_URL/user/$id").receiveOrNull()
        }
    }
}


//region utils
suspend inline fun <reified Ret> HttpResponse.receiveOrNull(): Ret? {
    return if (this.status.isSuccess()) {
        this.receive()
    } else {
        println(this.status)
        null
    }
}
//endregion
