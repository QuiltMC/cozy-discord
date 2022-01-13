package org.quiltmc.community.modes.devtools.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.commands.converters.impl.roleList
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import dev.kord.core.behavior.MemberBehavior
import dev.kord.rest.builder.message.create.allowedMentions
import org.koin.core.component.inject
import org.quiltmc.community.TOOLCHAIN_GUILD
import org.quiltmc.community.database.collections.TeamCollection
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.database.entities.Team
import org.quiltmc.community.github.GhTeam
import org.quiltmc.community.github.GhUser

class TeamsExtension : Extension() {
    override val name: String = "teams"

    private val userFlagsCollection: UserFlagsCollection by inject()
    private val teamCollection: TeamCollection by inject()

    override suspend fun setup() {
        publicSlashCommand {
            name = "team"
            description = "Manage teams on the Quilt GitHub"

            guild(TOOLCHAIN_GUILD)

            publicSubCommand(::LinkTeamArguments) {
                name = "link"
                description = "Link a team between Discord and GitHub, so that members can be added to it. Existing members will not be affected."

                action {
                    // Find the team on gh
                    val queriedTeam = GhTeam.get(arguments.slug)

                    if (queriedTeam == null) {
                        respond {
                           content = "A team with the slug `${arguments.slug}` could not be found!"
                        }

                        return@action
                    } else if (queriedTeam.permission != "admin") {
                        respond {
                            content = "Cozy does not have permission to administrate the `${arguments.slug}` team! " +
                                    "You can only link teams that Cozy has sufficient permissions to manage."
                        }

                        return@action
                    }

                    teamCollection.set(Team(arguments.role.id, arguments.managers.map { role -> role.id }, queriedTeam.id))

                    respond {
                        // todo embed
                        respond {
                            content = "Linked successfully."
                        }
                    }
                }
            }

            publicSubCommand(::ViewTeamArguments) {
                name = "view"
                description = "View a team, its linked and unlinked members, and the roles that can manage it"
            }

            publicSubCommand(::AddMemberArguments) {
                name = "addMember"
                description = "Add a a member to a team."

                action {
                    val team = teamCollection.get(arguments.team.id)

                    if (team == null) {
                        respondEphemeral {
                            content = "That role isn't a Cozy-managed team!"
                        }

                        return@action
                    }

                    if (!isManager(team, user.asMember(guild!!.id))) {
                        respondEphemeral {
                           content = "You must be a manager of ${arguments.team.mention} to add or remove its members!"
                            allowedMentions { }
                        }

                        return@action
                    }

                    val userGhId = userFlagsCollection.get(arguments.user.id)?.githubId
                    val userGhLogin = userGhId?.let { GhUser.get(it)?.login }

                    if (userGhId == null || userGhLogin == null) {
                        respond {
                           content = "${arguments.user.mention} needs to link their Github account using `/github link` " +
                                   "before they can be added to a team!"
                            // user is intentionally pinged so they link their GH account
                        }

                        return@action
                    }


                    val result = GhTeam.addMember(team.databaseId, userGhLogin)

                    if (!result.added) {
                        respond {
                            content = "Unable to add user: ${result.statusCode}"
                        }
                    } else {
                        if (result.pending) {
                            respond {
                                content = "${arguments.user.mention} was successfully invited to join ${arguments.team.mention}. " +
                                        "They can accept the invitation here: https://github.com/quiltmc/invitation"

                                allowedMentions {
                                    // intentionally ping the user so that they know to accept the invite
                                    users.add(arguments.user.id)
                                }
                            }
                        } else {
                            respond {
                               content = "Successfully added ${arguments.user.mention} to ${arguments.team.mention}."
                                allowedMentions { }
                            }
                        }
                    }
                }
            }

            publicSubCommand(::RemoveMemberArguments) {
                name = "removeMember"
                description = "Remove a member from a team"
            }

            publicSubCommand(::AddManagersArguments) {
                name = "addManagers"
                description = "Add teams that can add/remove members from a specific team"
            }

            publicSubCommand(::RemoveManagersArguments) {
                name = "removeManagers"
                description = "Remove teams that can add/remove members from a specific team"
            }
        }
    }

    private suspend fun isManager(team: Team, member: MemberBehavior) : Boolean {
        return member.asMember().roleIds.any {
            it in team.managers
        }
    }

    inner class LinkTeamArguments : Arguments() {
        val slug by string("slug", "The slug of the team on GitHub")
        val role by role("role", "The role of this team on Discord")
        val managers by roleList("managers", "The teams that may add and remove members from this team.")
    }

    inner class ViewTeamArguments : Arguments() {
        val role by role("role", "The role of the team")
    }

    inner class AddMemberArguments : Arguments() {
        val user by user("user", "The user to add")
        val team by role("team", "The team to add the user to")
    }

    inner class RemoveMemberArguments : Arguments() {
        val user by user("user", "The user to remove")
        val team by role("team", "The team to remove the user from")
    }

    inner class AddManagersArguments : Arguments() {
        val team by role("team", "The team to add managers to")
        val managers by roleList("managers", "The managers to add to the team")
    }

    inner class RemoveManagersArguments : Arguments() {
        val team by role("team", "The team to remove managers from")
        val managers by roleList("managers", "The managers to remove from the team")
    }

}