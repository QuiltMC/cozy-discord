@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.quiltmc.community.modes.quilt.extensions.suggestions.SuggestionStatus

@Serializable
@Suppress("ConstructorParameterNaming")  // MongoDB calls it that...
data class Suggestion(
    val _id: Snowflake,

    var comment: String? = null,
    var status: SuggestionStatus = SuggestionStatus.Open,
    var message: Snowflake? = null,

    var text: String,

    val owner: Snowflake,
    val ownerAvatar: String?,
    val ownerName: String,

    val positiveVoters: MutableList<Snowflake> = mutableListOf(),
    val negativeVoters: MutableList<Snowflake> = mutableListOf(),

    val isTupper: Boolean = false
) {
    val positiveVotes get() = positiveVoters.size
    val negativeVotes get() = negativeVoters.size
    val voteDifference get() = positiveVotes - negativeVotes
}
