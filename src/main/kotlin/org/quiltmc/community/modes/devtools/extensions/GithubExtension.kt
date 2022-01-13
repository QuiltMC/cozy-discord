package org.quiltmc.community.modes.devtools.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import org.koin.core.component.inject
import org.quiltmc.community.TOOLCHAIN_GUILD
import org.quiltmc.community.database.collections.UserFlagsCollection
import org.quiltmc.community.github.DatabaseId
import org.quiltmc.community.github.GhUser

class GithubExtension : Extension() {
    override val name = "github"

    private val userFlagsCollection: UserFlagsCollection by inject()



    override suspend fun setup() {
        // toolchain only
        ephemeralSlashCommand {
            name = "github"
            description = "Manage Cozy's GitHub and Discord integration"

            guild(TOOLCHAIN_GUILD)

            // anyone can use this command

            ephemeralSubCommand(::LinkUserArgs) {
                name = "link"
                description = "Link your GitHub account to your Discord account. Used by Cozy for adding users to teams"

                action {
                    val userId: DatabaseId? = GhUser.get(arguments.login)?.id

                    if (userId == null) {
                        respond {
                            content = "User with login `${arguments.login}` not found!"
                        }

                        return@action
                    }

                    val flags = userFlagsCollection.getOrCreate(this.user.id)
                    flags.githubId = userId
                    flags.save()
                    respond {
                        // TODO: pull more from this query for an embed
                        // TODO: log this somewhere
                        content = "Linked your GitHub account with user login `${arguments.login}`"
                    }
                }
            }

            ephemeralSubCommand() {
                name = "unlink"
                description = "Unlink your GitHub account from your Discord user on Cozy"

                action {
                    // I have no idea how this API works with deletions, so I'm going to go the safe route.
                    val flags = userFlagsCollection.getOrCreate(this.user.id)
                    flags.githubId = null
                    flags.save()

                    respond {
                        // TODO: detect if one was actually removed or not
                        content = "Unlinked your GitHub account, if one was linked."
                    }

                }
            }

        }

        // TODO: github's rest api doesn't support deleting issues,
        //      so we'll have to manually use graphql to reimplement this
//        // both guilds
//        for (guildId in GUILDS) {
//            ephemeralSlashCommand {
//                name = "github-issue"
//                description = "Manage issues on GitHub"
//
//                guild(guildId)
//
//                check { hasBaseModeratorRole() }
//
//                ephemeralSubCommand(::DeleteIssueArgs) {
//                    name = "delete"
//                    description = "Delete the given issue"
//
//                    action {
//                        val repo = githubGraphQlClient
//                            .execute(FindIssueId(FindIssueId.Variables(arguments.repo, arguments.issue)))
//                            .data
//                            ?.repository
//
//                        when {
//                            repo == null -> respond {
//                                content = "Repository ${arguments.repo} not found!"
//                            }
//
//                            repo.pullRequest != null -> respond {
//                                content = "#${arguments.issue} appears to be a pull request. " +
//                                        "GitHub does not allow deleting pull requests! " +
//                                        "Please contact GitHub Support."
//                            }
//
//                            repo.issue != null -> {
//                                // try to delete the issue
//                                val response = githubGraphQlClient.execute(DeleteIssue(DeleteIssue.Variables(repo.issue.id)))
//
//                                if (response.errors.isNullOrEmpty()) {
//                                    respond {
//                                        content = "Issue #${arguments.issue} in repository ${arguments.repo}" +
//                                                " deleted successfully!"
//                                    }
//
//                                    // log the deletion
//                                    // TODO: GitHub's webhooks do provide a way to automatically log all deletions
//                                    //      through a webhook, but there is no filter.
//                                    //      In the future, we could set something up on Cozy to automatically
//                                    //      filter the github webhook to create an action log.
//                                    getGitHubLogChannel()?.createEmbed {
//                                        val issue = repo.issue
//
//                                        title = "Issue Deleted: ${issue.title}"
//                                        color = DISCORD_RED
//                                        description = issue.body
//
//                                        field {
//                                            val loginAndId = getActorLoginAndId(issue.author!!)
//
//                                            name = "Issue Author"
//                                            value = "${loginAndId.first} (${loginAndId.second})"
//                                        }
//
//                                        userField(user, "Moderator")
//
//                                        if (arguments.reason != null) {
//                                            field {
//                                                name = "Reason"
//                                                value = arguments.reason!!
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    respond {
//                                        // TODO: need a prettier way to report errors
//                                        content = "Could not delete issue due to errors:"
//                                        response.errors!!.forEach {
//                                            content += "\n" + it.message
//                                        }
//                                    }
//                                }
//                            }
//
//                            else -> respond {
//                                content = "Could not find issue #${arguments.issue} in repository ${arguments.repo}"
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

//    inner class DeleteIssueArgs : Arguments() {
//        val repo by string("repository", "The name of the repository")
//        val issue by int("issue", "The number of the issue or pull request to delete")
//        val reason by optionalString(
//            "reason",
//            "A short explanation of why this issue is being deleted"
//        )
//    }

    inner class LinkUserArgs : Arguments() {
        val login by string("slug", "The slug (username) of the GitHub account to link")
    }
}
