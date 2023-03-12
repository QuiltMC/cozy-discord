/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.koin.core.component.inject

public class MatchRegexSerializer : KSerializer<Regex>, KordExKoinComponent {
	private val bot: ExtensibleBot by inject()

	private val extension: LogParserExtension get() =
		bot.findExtension()!!

	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Regex", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): Regex {
		val string = decoder.decodeString().replaceVariables()

		return string.toRegex(RegexOption.IGNORE_CASE)
	}

	override fun serialize(encoder: Encoder, value: Regex) {
		val string = value.toString().replaceVariables(true)

		encoder.encodeString(string)
	}

	private fun String.replaceVariables(reverse: Boolean = false): String {
		var result = this

		extension.pastebinConfig.variables.forEach { (key, value) ->
			result = if (reverse) {
				result.replace(value, key)
			} else {
				result.replace(key, value)
			}
		}

		return result
	}
}
