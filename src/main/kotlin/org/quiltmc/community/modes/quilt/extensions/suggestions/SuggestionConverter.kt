/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(KordPreview::class)

package org.quiltmc.community.modes.quilt.extensions.suggestions

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.Argument
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.converters.Validator
import com.kotlindiscord.kord.extensions.modules.annotations.converters.Converter
import com.kotlindiscord.kord.extensions.modules.annotations.converters.ConverterType
import com.kotlindiscord.kord.extensions.parser.StringParser
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.interaction.OptionValue
import dev.kord.core.entity.interaction.StringOptionValue
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import org.koin.core.component.inject
import org.quiltmc.community.database.collections.SuggestionsCollection
import org.quiltmc.community.database.entities.Suggestion

@Converter(
	names = ["suggestion"],
	types = [ConverterType.SINGLE, ConverterType.OPTIONAL],
)
class SuggestionConverter(
	override var validator: Validator<Suggestion> = null
) : SingleConverter<Suggestion>() {
	override val signatureTypeString: String = "Suggestion ID"

	private val suggestions: SuggestionsCollection by inject()

	override suspend fun parse(parser: StringParser?, context: CommandContext, named: String?): Boolean {
		val arg: String = named ?: parser?.parseNext()?.data ?: return false

		try {
			val snowflake = Snowflake(arg)

			this.parsed = suggestions.get(snowflake)
				?: suggestions.getByMessage(snowflake)
						?: throw DiscordRelayedException("Unknown suggestion ID: $arg")
		} catch (e: NumberFormatException) {
			throw DiscordRelayedException("Unknown suggestion ID: $arg")
		}

		return true
	}

	override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
		StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }

	override suspend fun parseOption(context: CommandContext, option: OptionValue<*>): Boolean {
		val arg = (option as? StringOptionValue)?.value ?: return false

		try {
			val snowflake = Snowflake(arg)

			this.parsed = suggestions.get(snowflake)
				?: suggestions.getByMessage(snowflake)
						?: throw DiscordRelayedException("Unknown suggestion ID: $arg")
		} catch (e: NumberFormatException) {
			throw DiscordRelayedException("Unknown suggestion ID: $arg")
		}

		return true
	}
}
