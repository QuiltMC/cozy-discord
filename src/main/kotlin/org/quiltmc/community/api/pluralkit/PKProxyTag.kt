package org.quiltmc.community.api.pluralkit

import kotlinx.serialization.Serializable

@Serializable
data class PKProxyTag(
    val prefix: String?,
    val suffix: String?
)
