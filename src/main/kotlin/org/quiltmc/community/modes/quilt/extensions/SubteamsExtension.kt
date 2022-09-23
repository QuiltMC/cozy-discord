/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.rest.builder.message.create.allowedMentions
import org.koin.core.component.inject
import org.quiltmc.community.TOOLCHAIN_GUILD
import org.quiltmc.community.database.collections.TeamCollection
import org.quiltmc.community.database.entities.Team

class SubteamsExtension : Extension() {
	override val name: String = "subteams"

	private val teamColl: TeamCollection by inject()

	override suspend fun setup() {
		publicSlashCommand {
			name = "team"
			description = "Manage your teams"

			allowInDms = false

			guild(TOOLCHAIN_GUILD)

			publicSubCommand(::TeamArguments) {
				name = "add"
				description = "Add someone to your team or any subteam"

				action {
					if (this.member?.asMemberOrNull()?.mayManageRole(arguments.role) == true) {
						arguments.targetUser.addRole(
							arguments.role.id,
							"${this.user.asUserOrNull()?.tag ?: this.user.id} used /team add"
						)

						respond {
							content = "Successfully added ${arguments.targetUser.mention} to ${arguments.role.mention}."

							allowedMentions { }
						}
					} else {
						respond {
							content = "Your team needs to be above ${arguments.role.mention} in order to add anyone " +
									"to it."

							allowedMentions { }
						}
					}
				}
			}

			publicSubCommand(::TeamArguments) {
				name = "remove"
				description = "Remove someone from your team or any subteam"

				action {
					if (this.member?.asMemberOrNull()?.mayManageRole(arguments.role) == true) {
						arguments.targetUser.removeRole(
							arguments.role.id,
							"${this.user.asUserOrNull()?.tag ?: this.user.id} used /team remove"
						)
						respond {
							content = "Successfully removed ${arguments.targetUser.mention} from " +
									"${arguments.role.mention}."

							allowedMentions { }
						}
					} else {
						respond {
							content = "Your team needs to be above ${arguments.role.mention} in order to remove " +
									"anyone from it."

							allowedMentions { }
						}
					}
				}
			}
		}

		publicSlashCommand {
			name = "manage-teams"
			description = "Change which roles can manage each other"

			allowInDms = false

			guild(TOOLCHAIN_GUILD)

			check { hasPermission(Permission.Administrator) }

			ephemeralSubCommand(::ManageTeamAllowArguments) {
				name = "allow"
				description = "Allow a role to manage another using /team"

				action {
					teamColl.set(
						Team(
							_id = arguments.inferior.id,
							parent = arguments.superior.id
						)
					)

					respond {
						content = "${arguments.superior.mention} can now manage ${arguments.inferior.mention}"

						allowedMentions { }
					}
				}
			}

			ephemeralSubCommand(::ManageTeamDisallowArguments) {
				name = "disallow"
				description = "Prevent a role from being managed by another again"

				action {
					teamColl.delete(arguments.role.id)

					respond {
						content = "${arguments.role.mention} can no longer be managed using /team."

						allowedMentions { }
					}
				}
			}

			ephemeralSubCommand {
				name = "list-relationships"
				description = "List all the relationships between roles"

				action {
					respond {
						content = teamColl.getAll().joinToString("\n") { "<@&${it._id.value}> is managed by <@&${it.parent.value}>" }
					}
				}
			}
		}
	}

	inner class TeamArguments : Arguments() {
		val role by role {
			name = "team"
			description = "Which team to add"

			requiredGuild = { TOOLCHAIN_GUILD }
		}

		val targetUser by member {
			name = "user"
			description = "Who to add to the team"

			requiredGuild = { TOOLCHAIN_GUILD }
		}
	}

	inner class ManageTeamAllowArguments : Arguments() {
		val superior by role {
			name = "superior"
			description = "The superior role"

			requiredGuild = { TOOLCHAIN_GUILD }
		}

		val inferior by role {
			name = "inferior"
			description = "The inferior role"

			requiredGuild = { TOOLCHAIN_GUILD }
		}
	}

	inner class ManageTeamDisallowArguments : Arguments() {
		val role by role {
			name = "role"
			description = "Role to disallow managing for"

			requiredGuild = { TOOLCHAIN_GUILD }
		}
	}

	private suspend fun Member.mayManageRole(role: Role): Boolean =
		teamColl.getParents(role.id).any { it in this.roleIds }
}
