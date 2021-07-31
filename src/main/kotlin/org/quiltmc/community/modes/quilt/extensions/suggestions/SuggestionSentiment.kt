package org.quiltmc.community.modes.quilt.extensions.suggestions

import com.kotlindiscord.kord.extensions.commands.slash.converters.ChoiceEnum
import kotlinx.serialization.Serializable

@Serializable
enum class SuggestionSentiment(override val readableName: String) : ChoiceEnum {
    Positive("Positive"),
    Neutral("Neutral"),
    Negative("Negative"),
}
