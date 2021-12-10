package org.quiltmc.community.modes.devtools.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.koin.core.component.inject
import org.quiltmc.community.TOOLCHAIN_GUILD
import org.quiltmc.community.database.collections.TeamCollection
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.database.entities.Team
import org.quiltmc.community.githubGraphQlClient
import org.quiltmc.community.githubHttpClient
import quilt.ghgen.FindTeamDatabaseId

class TeamsExtension : Extension() {
    override val name: String = "teams"

    private val QUILTMC_DATABASE_ID = 78571508
    private val userFlagsCollection: UserFlagsCollection by inject()
    private val teamCollection: TeamCollection by inject()

    // TODO: permissions, guards, etc, oh my!
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
                    val queriedTeam = githubGraphQlClient.execute(FindTeamDatabaseId(FindTeamDatabaseId.Variables(arguments.slug)))
                        .data
                        ?.organization
                        ?.team

                    if (queriedTeam == null) {
                        respond {
                           content = "A team with the slug `${arguments.slug}` could not be found!"
                        }

                        return@action
                    } else if (!queriedTeam.viewerCanAdminister) {
                        respond {
                            content = "Cozy does not have permission to administrate the `${arguments.slug}` team! " +
                                    "You can only link teams that Cozy has sufficient permissions to manage."
                        }

                        return@action
                    }

                    teamCollection.set(Team(arguments.role.id, arguments.managers.map { role -> role.id }, queriedTeam.databaseId!!))

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
                        respond {
                            content = "That role isn't a Cozy-managed team!"
                        }

                        return@action
                    }


                    val response: HttpResponse =  this.githubHttpClient.put("https://api.github.com/orgs/$QUILTMC_DATABASE_ID/teams/${team.databaseId}")
                    val body: String = response.receive()

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