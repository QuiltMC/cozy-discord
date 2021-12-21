package org.quiltmc.community.github

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonTransformingSerializer
import org.litote.kmongo.serialization.configuration

const val ACCEPT = "application/vnd.github.v3+json"
val serializer = Json {
    encodeDefaults = false
}
