package org.quiltmc.community.modes.devtools.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import com.kotlindiscord.kord.extensions.types.respondPublic
import dev.kord.core.behavior.MemberBehavior
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.create.embed
import org.koin.core.component.inject
import org.quiltmc.community.GH_COZY_ID
import org.quiltmc.community.TOOLCHAIN_ADMIN_ROLE
import org.quiltmc.community.TOOLCHAIN_GUILD
import org.quiltmc.community.database.collections.TeamCollection
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.database.entities.Team
import org.quiltmc.community.github.GhTeam
import org.quiltmc.community.github.GhUser
import org.quiltmc.community.hasAdminRole

class TeamsExtension : Extension() {
    override val name: String = "teams"

    private val userFlagsCollection: UserFlagsCollection by inject()
    private val teamCollection: TeamCollection by inject()

    override suspend fun setup() {
        publicSlashCommand {
            name = "team"
            description = "Manage teams on the Quilt GitHub"

            guild(TOOLCHAIN_GUILD)

            ephemeralSubCommand(::LinkTeamArguments) {
                name = "link"
                description = "Link a team between Discord and GitHub, allowing syncing of members."
                guild(TOOLCHAIN_GUILD)
                check {
                    hasAdminRole()
                }
                action {
                    // Find the team on gh
                    val queriedTeam = GhTeam.get(arguments.slug)

                    if (queriedTeam == null) {
                        respond {
                           content = "A team with the slug `${arguments.slug}` could not be found!"
                        }

                        return@action
                    }

                    teamCollection.set(Team(arguments.role.id, ArrayList(), queriedTeam.id))

                    respond {
                       content = "Linked successfully"
                    }
                }
            }

            publicSubCommand(::ViewTeamArguments) {
                name = "view"
                description = "View a team, its linked and unlinked members, and the roles that can manage it"
                guild(TOOLCHAIN_GUILD)
                action {
                    val team = teamCollection.get(arguments.role.id)

                    if (team == null) {
                        respondEphemeral {
                            content = "That role isn't a Cozy-managed team!"
                        }

                        return@action
                    }
                    val ghTeam = GhTeam.get(team.databaseId)
                    if (ghTeam == null) {
                        respond {
                            content = "Unable to get that team's Github information!"
                        }
                        return@action
                    }

                    val ghMembers = ghTeam.members().filter { it.id.toString() != GH_COZY_ID }

                    val list = StringBuilder()

                    for (ghMember in ghMembers) {
                        val snowflake = userFlagsCollection.getByGithubId(ghMember.id)?._id
                        val discordMember = snowflake?.let { guild!!.getMemberOrNull(it) }

                        if (discordMember != null) {
                            list.append(discordMember.mention)
                        } else {
                            list.append("**Not Synced**")
                        }

                        list.append(" ([${ghMember.login}](https://github.com/${ghMember.login}))\n")
                    }

                    respond {
                        embed {
                            title = "Team: ${ghTeam.name}"
                            description = list.toString()
                            thumbnail {
                                url = "https://avatars.githubusercontent.com/t/${ghTeam.id}?s=256"
                            }
                        }
                    }
                }
            }

            publicSubCommand(::AddMemberArguments) {
                name = "add-member"
                description = "Add a a member to a team."
                guild(TOOLCHAIN_GUILD)
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
                           content = "${arguments.user.mention} needs to link their Github account " +
                                   "using `/github link` before they can be added to a team!"
                            // user is intentionally pinged so they link their GH account
                        }

                        return@action
                    }

                    val result = GhTeam.addMember(team.databaseId, userGhLogin)

                    if (!result.added) {
                        respondEphemeral {
                            content = "Unable to add user: ${result.statusCode}"
                        }
                    } else {
                        if (result.pending) {
                            respond {
                                content = "${arguments.user.mention} was successfully invited to join " +
                                        "${arguments.team.mention}. They can accept the invitation here: " +
                                        "https://github.com/quiltmc/invitation"

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
                name = "remove-member"
                description = "Remove a member from a team"
                guild(TOOLCHAIN_GUILD)
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
                        respondEphemeral {
                            content = "${arguments.user.mention} does not have a Github account linked!"
                        }

                        return@action
                    }

                    GhTeam.removeMember(team.databaseId, userGhLogin)
                    respondPublic {
                        content = "Successfully removed ${arguments.user.mention} from ${arguments.team.mention}."
                        allowedMentions { }
                    }
                }
            }

            ephemeralSubCommand(::AddManagerArguments) {
                name = "add-manager"
                description = "Add a role that can add/remove members from a specific team"
                guild(TOOLCHAIN_GUILD)
                check {
                    hasAdminRole()
                }
                action {
                    val team = teamCollection.get(arguments.team.id)

                    if (team == null) {
                        respond {
                            content = "That role isn't a Cozy-managed team!"
                        }

                        return@action
                    }

                    if (team.managers.contains(arguments.manager.id)) {
                        respond {
                            content = "${arguments.manager.mention} is already a manager of ${arguments.team.mention}"
                            allowedMentions {}
                        }
                        return@action
                    }

                    team.managers.add(arguments.manager.id)
                    respond {
                        content = "Successfully added ${arguments.manager.mention} as a manager of ${arguments.team.mention}"
                        allowedMentions { }
                    }
                    team.save()
                }
            }

            publicSubCommand(::RemoveManagerArguments) {
                name = "remove-manager"
                description = "Remove a role that can add/remove members from a specific team"
                guild(TOOLCHAIN_GUILD)
                check {
                    hasAdminRole()
                }
                action {
                    val team = teamCollection.get(arguments.team.id)

                    if (team == null) {
                        respond {
                            content = "That role isn't a Cozy-managed team!"
                        }

                        return@action
                    }

                    if (!team.managers.remove(arguments.manager.id)) {
                        respond {
                            content = "${arguments.manager.mention} is not a manager of ${arguments.team.mention}"
                        }
                    } else {
                        respond {
                            content = "${arguments.manager.mention} was removed" +
                                    " as a manager of ${arguments.team.mention}"
                        }
                    }

                    team.save()
                }
            }
        }
    }

    private suspend fun isManager(team: Team, member: MemberBehavior): Boolean {
        return member.asMember().roleIds.any {
            it in team.managers || it == TOOLCHAIN_ADMIN_ROLE
        }
    }

    inner class LinkTeamArguments : Arguments() {
        val slug by string("slug", "The slug of the team on GitHub")
        val role by role("role", "The role of this team on Discord")
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

    inner class AddManagerArguments : Arguments() {
        val team by role("team", "The team to add a manager to")
        val manager by role("managers", "The manager to add to the team")
    }

    inner class RemoveManagerArguments : Arguments() {
        val team by role("team", "The team to remove managers from")
        val manager by role("managers", "The manager to remove from the team")
    }
}
