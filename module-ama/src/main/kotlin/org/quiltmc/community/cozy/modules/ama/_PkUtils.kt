/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.ama

import dev.kord.common.Color
import dev.kord.common.entity.optional.Optional
import dev.kord.core.behavior.UserBehavior
import dev.kordex.core.utils.envOrNull
import dev.kordex.core.utils.getKoin
import dev.kordex.modules.pluralkit.api.PKMemberPrivacy
import dev.kordex.modules.pluralkit.api.PKProxyTag
import dev.kordex.modules.pluralkit.api.PKSystem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.quiltmc.community.cozy.modules.ama.data.AmaData
import kotlin.time.Duration.Companion.seconds

private const val MEMBER_ID_LENGTH = 5
private val uuidRegex = Regex("^[a-fA-F0-9]{8}(?:-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}$")

private val baseUrl = envOrNull("PLURALKIT_BASE_URL") ?: "https://api.pluralkit.me/v2"

private val client = HttpClient {
	install(ContentNegotiation) {
		json(
			Json { ignoreUnknownKeys = true },
			ContentType.Any
		)
	}
}

public sealed class PKResult<out T> {
	public object SystemNotFound : PKResult<Nothing>()
	public object SystemNotAccessible : PKResult<Nothing>()
	public object MemberNotFound : PKResult<Nothing>()
	public object MemberNotFromUser : PKResult<Nothing>()
	public object NotPermitted : PKResult<Nothing>()
	public object NoFronter : PKResult<Nothing>()
	public data class Success<T>(val result: T) : PKResult<T>()
}

// Using a different PKMember than provided by kordex because it doesn't have the "last message timestamp"
// or (not documented) "system" fields
@Serializable
public data class PKMember(
	val id: String,
	val uuid: String,
	val system: Optional<String> = Optional.Missing(),
	val name: String,
	@SerialName("display_name")
	val displayName: String?,
	@Serializable(with = ColorHexCodeSerializer::class)
	val color: Color?,
	val pronouns: String?,
	@SerialName("avatar_url")
	val avatarUrl: String?,
	@SerialName("webhook_avatar_url")
	val webhookAvatarUrl: String?,
	val banner: String?,
	val description: String?,
	val created: Instant?,
	@SerialName("proxy_tags")
	val proxyTags: List<PKProxyTag>,
	@SerialName("keep_proxy")
	val keepProxy: Boolean,
	@SerialName("autoproxy_enabled")
	val autoproxyEnabled: Boolean?,
	@SerialName("message_count")
	val messageCount: Int?,
	@SerialName("last_message_timestamp")
	val lastMessageTimestamp: Instant?,
	val privacy: PKMemberPrivacy?,
)

@Serializable
public data class PKMemberSwitch(
	val id: String,
	val timestamp: Instant,
	val members: List<PKMember>
)

private suspend inline fun get(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
	while (true) {
		val response = client.get(url, block)
		if (response.status == HttpStatusCode.TooManyRequests) {
			val retryAfter = response.headers["X-RateLimit-Reset"]?.let { Instant.parse(it) }
			if (retryAfter != null) {
				val waitTime = retryAfter.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()
				if (waitTime > 0) {
					delay(waitTime)
				}
			} else {
				delay(1.seconds)
			}
		} else {
			return response
		}
	}
}

public suspend fun UserBehavior.getPluralKitSystem(): PKResult<PKSystem> {
	val systemRequest = client.get("$baseUrl/systems/$id")

	if (systemRequest.status == HttpStatusCode.NotFound) {
		return PKResult.SystemNotFound
	} else if (systemRequest.status != HttpStatusCode.OK) {
		return PKResult.SystemNotAccessible
	}

	return PKResult.Success(systemRequest.body())
}

public suspend fun UserBehavior.getPluralKitMember(text: String): PKResult<PKMember> {
	val system = getPluralKitSystem()

	if (system !is PKResult.Success) {
		@Suppress("UNCHECKED_CAST")
		return system as PKResult<PKMember>
	}

	val membersRequest = get("$baseUrl/systems/${system.result.id}/members")
	if (membersRequest.status == HttpStatusCode.Forbidden) {
		return PKResult.NotPermitted
	} else if (membersRequest.status != HttpStatusCode.OK) {
		return PKResult.SystemNotAccessible
	}

	val members: List<PKMember> = membersRequest.body()

	val member = members.firstOrNull { it.name.equals(text, ignoreCase = true) }
	if (member != null) {
		return PKResult.Success(member)
	}

	if (text.matches(uuidRegex) || text.length == MEMBER_ID_LENGTH) {
		val memberRequest = client.get("$baseUrl/members/$text")
		if (memberRequest.status == HttpStatusCode.OK) {
			val result: PKMember = memberRequest.body()
			if (result.system.value == system.result.id) {
				return PKResult.Success(result)
			} else if (result.system !is Optional.Missing) {
				return PKResult.MemberNotFromUser
			}
		}
	} else if (text.isBlank()) {
		if (getKoin().get<AmaData>().usePluralKitFronter(id)) {
			val fronter = getMostRecentFronter()
			if (fronter != null) {
				return PKResult.Success(fronter)
			}
		}
		return PKResult.NoFronter
	}

	return PKResult.MemberNotFound
}

public suspend fun UserBehavior.getFronters(): PKMemberSwitch? {
	val switchesRequest = client.get("$baseUrl/systems/$id/fronters")
	if (switchesRequest.status != HttpStatusCode.OK) {
		return null
	}

	return switchesRequest.body()
}

public suspend fun UserBehavior.getMostRecentFronter(): PKMember? {
	val fronters = getFronters() ?: return null

	if (fronters.members.size == 1) return fronters.members.single()

	return fronters.members.maxByOrNull { it.lastMessageTimestamp ?: Instant.DISTANT_PAST }
}
