/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public class MatchRegexSerializer : KSerializer<Regex> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Regex", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): Regex {
		val string = decoder.decodeString()

		return string.toRegex(RegexOption.IGNORE_CASE)
	}

	override fun serialize(encoder: Encoder, value: Regex) {
		val string = value.toString()

		encoder.encodeString(string)
	}
}
