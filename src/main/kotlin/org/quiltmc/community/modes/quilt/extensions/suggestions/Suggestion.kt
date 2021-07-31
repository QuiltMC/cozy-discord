@file:Suppress("DataClassShouldBeImmutable")  // Well, yes, but actually no.

package org.quiltmc.community.modes.quilt.extensions.suggestions

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class Suggestion(
    val id: String,

    var comment: String? = null,
    var status: SuggestionStatus = SuggestionStatus.Open,
    var message: Snowflake? = null,

    var text: String,

    var owner: String,
    var ownerAvatar: String?,
    var ownerName: String,

    val positiveVoters: MutableList<Snowflake> = mutableListOf(),
    val negativeVoters: MutableList<Snowflake> = mutableListOf(),

    val isTupper: Boolean = false
) {
    val positiveVotes get() = positiveVoters.size
    val negativeVotes get() = negativeVoters.size
    val voteDifference get() = positiveVotes - negativeVotes
}
