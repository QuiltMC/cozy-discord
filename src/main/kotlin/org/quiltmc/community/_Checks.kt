/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community

import com.kotlindiscord.kord.extensions.checks.*
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.utils.hasPermission
import com.kotlindiscord.kord.extensions.utils.translate
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.entity.channel.CategorizableChannel
import mu.KotlinLogging
import org.quiltmc.community.database.collections.ServerSettingsCollection

suspend fun CheckContext<*>.notInStaffChannel() {
	// TODO: This should be configurable but it's a band-aid for now
	anyGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.notInStaffChannel")

	val channel = channelFor(event)?.asChannelOfOrNull<CategorizableChannel>()
		?: return

	if (channel.id in STAFF_CHANNELS) {
		logger.failed("This is a staff channel.")
		fail("This is a staff channel.")
	}

	if (channel.categoryId in STAFF_CATEGORIES) {
		logger.failed("This is a staff channel.")
		fail("This is a staff channel.")
	}
}

suspend fun CheckContext<*>.inCommunity() {
	anyGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.inCommunity")

	val collection = getKoin().get<ServerSettingsCollection>()
	val settings = collection.getCommunity()

	if (settings == null) {
		logger.failed("Community server hasn't been configured yet.")
		fail("Community server hasn't been configured yet.")
	} else {
		inGuild(settings._id)
	}
}

suspend fun CheckContext<*>.notInCommunity() {
	anyGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.notInCommunity")

	val collection = getKoin().get<ServerSettingsCollection>()
	val settings = collection.getCommunity()

	if (settings == null) {
		logger.passed("Community server hasn't been configured yet.")
		pass()
	} else {
		notInGuild(settings._id)
	}
}

suspend fun CheckContext<*>.inToolchain() {
	anyGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.inToolchain")

	val collection = getKoin().get<ServerSettingsCollection>()
	val settings = collection.getToolchain()

	if (settings == null) {
		logger.failed("Toolchain server hasn't been configured yet.")
		fail("Toolchain server hasn't been configured yet.")
	} else {
		inGuild(settings._id)
	}
}

suspend fun CheckContext<*>.notInToolchain() {
	anyGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.notInToolchain")

	val collection = getKoin().get<ServerSettingsCollection>()
	val settings = collection.getToolchain()

	if (settings == null) {
		logger.passed("Toolchain server hasn't been configured yet.")
		pass()
	} else {
		notInGuild(settings._id)
	}
}

suspend fun CheckContext<*>.inCollab() {
	anyGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.inCollab")

	val collection = getKoin().get<ServerSettingsCollection>()
	val settings = collection.getCollab()

	if (settings == null) {
		logger.failed("Collab server hasn't been configured yet.")
		fail("Collab server hasn't been configured yet.")
	} else {
		inGuild(settings._id)
	}
}

suspend fun CheckContext<*>.notInCollab() {
	anyGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.notInCollab")

	val collection = getKoin().get<ServerSettingsCollection>()
	val settings = collection.getCollab()

	if (settings == null) {
		logger.passed("Collab server hasn't been configured yet.")
		pass()
	} else {
		notInGuild(settings._id)
	}
}

suspend fun CheckContext<*>.hasPermissionInMainGuild(perm: Permission) {
	anyGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.hasPermissionInMainGuild")
	val user = userFor(event)

	if (user == null) {
		logger.failed("Event did not concern a user.")
		fail()

		return
	}

	val guild = event.kord.getGuildOrNull(MAIN_GUILD)!!
	val member = guild.getMemberOrNull(user.id)

	if (member == null) {
		logger.failed("User is not on the main guild.")

		fail(
			translate(
				"checks.inGuild.failed",
				replacements = arrayOf(guild.name),
			)
		)

		return
	}

	if (member.hasPermission(perm)) {
		logger.passed()
	} else {
		logger.failed("User does not have permission: $perm")
		fail("Must have permission **${perm.translate(locale)}** on **${guild.name}**")
	}
}

suspend fun CheckContext<*>.inQuiltGuild() {
	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.inQuiltGuild")
	val guild = guildFor(event)

	if (guild == null) {
		logger.nullGuild(event)

		fail("Must be in one of the Quilt servers")
	} else {
		if (guild.id !in GUILDS) {
			fail("Must be in one of the Quilt servers")
		}
	}
}

suspend fun CheckContext<*>.hasBaseModeratorRole(includeCommunityManagers: Boolean = true) {
	inQuiltGuild()

	if (!passed) {
		return
	}

	if (this.passed) {  // They're on a Quilt guild
		val logger = KotlinLogging.logger("org.quiltmc.community.hasBaseModeratorRole")
		val member = memberFor(event)?.asMemberOrNull()

		if (member == null) {  // Shouldn't happen, but you never know
			logger.nullMember(event)

			fail()
		} else {
			val hasModeratorRole = member.roleIds.any { it in MODERATOR_ROLES }
			val hasCommunityManagerRole = member.roleIds.any { it in MANAGER_ROLES }

			if (!hasModeratorRole && (!hasCommunityManagerRole || !includeCommunityManagers)) {
				val roleDescription = if (includeCommunityManagers) "Moderator or Community Manager" else "Moderator"

				logger.failed("Member does not have a Quilt $roleDescription role")

				fail("Must be a Quilt $roleDescription")
			}
		}
	}
}

suspend fun CheckContext<*>.hasCommunityTeamRole() {
	inQuiltGuild()

	if (!passed) {
		return
	}

	if (this.passed) {  // They're on a Quilt guild
		val logger = KotlinLogging.logger("org.quiltmc.community.hasCommunityTeamRole")
		val member = memberFor(event)?.asMemberOrNull()

		if (member == null) {  // Shouldn't happen, but you never know
			logger.nullMember(event)

			fail()
		} else {
			val hasCommunityTeamRole = member.roleIds.any { it in COMMUNITY_TEAM_ROLES }

			if (!hasCommunityTeamRole) {
				logger.failed("Member does not have a Quilt Community Team role")

				fail("Must be a Quilt Community Team member")
			}
		}
	}
}

suspend fun CheckContext<*>.notHasBaseModeratorRole(includeCommunityManagers: Boolean = true) {
	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.notHasBaseModeratorRole")
	val member = memberFor(event)?.asMemberOrNull()

	if (member == null) {  // Not on a guild, fail.
		logger.nullMember(event)

		fail()
	} else {
		val hasModeratorRole = member.roleIds.any { it in MODERATOR_ROLES }
		val hasCommunityManagerRole = member.roleIds.any { it in MANAGER_ROLES }

		if (hasModeratorRole || (hasCommunityManagerRole && includeCommunityManagers)) {
			val roleDescription = if (includeCommunityManagers) "Moderator or Community Manager" else "Moderator"

			logger.failed("Member has a Quilt $roleDescription role")

			fail("Must **not** be a Quilt $roleDescription")
		}
	}
}

suspend fun CheckContext<*>.inReleaseChannel() {
	inQuiltGuild()

	if (!passed) {
		return
	}

	val logger = KotlinLogging.logger("org.quiltmc.community.inReleaseChannel")
	val message = messageFor(event)?.asMessageOrNull()

	if (message == null) {
		logger.nullMessage(event)

		fail()
	} else {
		if (message.channelId !in COMMUNITY_RELEASE_CHANNELS) {
			logger.failed("Message not in a release channel")

			fail("Message must be in a release channel")
		}
	}
}
