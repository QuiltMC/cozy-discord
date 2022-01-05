package org.quiltmc.community.api.pluralkit

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PKSystem(
    val id: String,
    val uuid: String,
    val name: String?, // PK docs are wrong
    val description: String?,
    val tag: String?,

    @SerialName("avatar_url")
    val avatarUrl: String?,

    val banner: String?,
    val color: String?, // PK docs are wrong
    val created: Instant,
    val timezone: String? = null,
    val privacy: PKSystemPrivacy?
)
