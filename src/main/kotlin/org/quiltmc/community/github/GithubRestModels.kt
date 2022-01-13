package org.quiltmc.community.github

import dev.kord.rest.request.HttpStatus
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
import org.quiltmc.community.GITHUB_TOKEN
/// This is a tiny wrapper around the Github REST API, providing only
/// what Cozy specifically needs.
/// It

private val client by lazy { HttpClient(engineFactory = CIO, block = {
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
private const val ORG_URL: String = "$GH_URL/organizations/$GH_ORG_ID/"
@Serializable
data class GhTeam(val name: String, val id: DatabaseId, val slug: String, val permission: String) {
    companion object {
        suspend fun get(slug: String?) : GhTeam? {
            if (slug == null) {
                return null
            }

            return client.get<HttpResponse>(url(slug)).receiveOrNull()
        }

        suspend fun addMember(teamId: DatabaseId, userLogin: String) : AddMemberResult {
            return addMember(url(teamId) + "memberships/$userLogin")
        }

        suspend fun addMember(teamSlug: String, userLogin: String) : AddMemberResult {
            return addMember(url(teamSlug) + "memberships/$userLogin")
        }

        private suspend fun addMember(url: String): AddMemberResult {
            val response: HttpResponse = client.put(url)
            return if (response.status.value == 200) {
                AddMemberResult(added = true, pending = response.receive<String>().contains("pending"), response.status) // hack
            } else {
                AddMemberResult(added = false, pending = false, response.status)
            }
        }

        private fun url(slug: String) : String {
            return "$ORG_URL/teams/$slug/"
        }
        private fun url(id: DatabaseId) : String {
            return "$ORG_URL/team/$id/"
        }

        data class AddMemberResult(val added: Boolean, val pending: Boolean, val statusCode: HttpStatusCode)
    }

    suspend fun addMember(userLogin: String) : AddMemberResult {
        return addMember(this.id, userLogin)
    }
}

@Serializable
data class GhUser(val login: String, val id: DatabaseId) {
    companion object {
        suspend fun get(login: String?) : GhUser? {
            if (login == null) {
                return null
            }

            return client.get<HttpResponse>("$GH_URL/users/$login").receiveOrNull()
        }

        suspend fun get(id: DatabaseId?) : GhUser? {
            if (id == null) {
                return null
            }

            // Undocumented endpoint! what fun!
            return client.get<HttpResponse>("$GH_URL/user/$id").receiveOrNull()
        }
    }
}


//region utils
suspend inline fun <reified Ret> HttpResponse.receiveOrNull() : Ret? {
    return if (this.status.value != 200) {
        null
    } else {
        this.receive()
    }
}
//endregion
