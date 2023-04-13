package org.quiltmc.community.modes.quilt.extensions.voting

import com.kotlindiscord.kord.extensions.checks.inChannel
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent

private val BUTTON_REGEX = "vote/([^/]+)/(.+)".toRegex()

class VotingExtension : Extension() {
	override val name: String = "voting"

	private val scheduler = Scheduler()

	override suspend fun setup() {
		event<ButtonInteractionCreateEvent> {
			check { inChannel(VotingConfig.VOTING_CHANNEL_ID) }
		}
	}

	override suspend fun unload() {
		scheduler.shutdown()
	}
}
