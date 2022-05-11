/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:UseSerializers(VersionSerializer::class)

package org.quiltmc.community.modes.quilt.extensions.logs.misc

import io.github.z4kn4fein.semver.Version
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object VersionSerializer : KSerializer<Version> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Version", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Version =
        Version.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Version) {
        encoder.encodeString(value.toString())
    }
}

@Serializable
data class LoaderVersion(
    val loader: LoaderElement,
)

@Serializable
data class LoaderElement(
    val separator: String,
    val build: Int,
    val maven: String,
    val version: Version
)
