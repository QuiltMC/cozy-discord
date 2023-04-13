package org.quiltmc.community.modes.quilt.extensions.voting

import com.kotlindiscord.kord.extensions.utils.envOrNull
import com.soywiz.korio.lang.parseInt
import dev.kord.common.entity.Snowflake
import org.quiltmc.community.COMMUNITY_MANAGER_ROLE
import org.quiltmc.community.COMMUNITY_MODERATOR_ROLE

object VotingConfig {
	val ALLOWED_ROLES = envOrNull("VOTING_ROLES_ALLOWED")
		?.split(",")
		?.map { Snowflake(it) }
		?: listOf(
			COMMUNITY_MANAGER_ROLE,
			COMMUNITY_MODERATOR_ROLE,
			Snowflake("863710574650327100"),  // Community Team
		)

	val EXCLUDED_ROLES = envOrNull("VOTING_ROLES_EXCLUDED")
		?.split(",")
		?.map { Snowflake(it) }
		?: listOf(
			Snowflake("1048994600527867955"),  // CT Trainee
			Snowflake("863766369312833546"),  // Mod Trainee
		)

	val TOTAL_VOTES_PERCENT = envOrNull("VOTING_TOTAL_PERCENT")
		?.parseInt()
		?: 50

	val POSITIVE_VOTES_PERCENT = envOrNull("VOTING_POSITIVE_PERCENT")
		?.parseInt()
		?: 70

	val VOTING_CHANNEL_ID = envOrNull("VOTING_CHANNEL")
		?.let { Snowflake(it) }
		?: Snowflake("834149163977539613")  // staff-democracy
}
