package org.quiltmc.community.modes.devtools.extensions

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.createEmbed
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.defaultRequest
import io.ktor.client.request.header
import org.koin.core.component.inject
import org.quiltmc.community.*
import org.quiltmc.community.database.collections.LinkedUserCollection
import org.quiltmc.community.database.entities.LinkedUser
import org.quiltmc.community.github.NodeId
import quilt.ghgen.DeleteIssue
import quilt.ghgen.FindIssueId
import quilt.ghgen.FindUserNodeId
import quilt.ghgen.findissueid.*
import java.net.URL

class GithubExtension : Extension() {
    override val name = "github"

    private val linkedUserCollection: LinkedUserCollection by inject()


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
                    val userNodeId: NodeId? = graphQlClient
                        .execute(FindUserNodeId(FindUserNodeId.Variables(arguments.login)))
                        .data
                        ?.user
                        ?.id

                    if (userNodeId == null) {
                        respond {
                            content = "User with login `${arguments.login}` not found!"
                        }

                        return@action
                    }

                    linkedUserCollection.set(LinkedUser(this.user.id, userNodeId))

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
                    // I have no idea how this API works with deletions so I'm going to go the safe route.
                    val linked = linkedUserCollection.get(user.id)

                    if (linked == null) {
                        respond {
                            content = "You don't have a GitHub account linked!"
                        }

                        return@action
                    } else {
                        linkedUserCollection.delete(user.id)

                        // TODO: fancy stuff, log, etc
                        respond {
                            content = "Unlinked your GitHub account"
                        }
                    }
                }
            }

        }

        // both guilds
        for (guildId in GUILDS) {
            ephemeralSlashCommand {
                name = "github-issue"
                description = "Manage issues on GitHub"

                guild(guildId)

                check { hasBaseModeratorRole() }

                ephemeralSubCommand(::DeleteIssueArgs) {
                    name = "delete"
                    description = "Delete the given issue"

                    action {
                        val repo = graphQlClient
                            .execute(FindIssueId(FindIssueId.Variables(arguments.repo, arguments.issue)))
                            .data
                            ?.repository

                        when {
                            repo == null -> respond {
                                content = "Repository ${arguments.repo} not found!"
                            }

                            repo.pullRequest != null -> respond {
                                content = "#${arguments.issue} appears to be a pull request. " +
                                        "GitHub does not allow deleting pull requests! " +
                                        "Please contact GitHub Support."
                            }

                            repo.issue != null -> {
                                // try to delete the issue
                                val response = graphQlClient.execute(DeleteIssue(DeleteIssue.Variables(repo.issue.id)))

                                if (response.errors.isNullOrEmpty()) {
                                    respond {
                                        content = "Issue #${arguments.issue} in repository ${arguments.repo}" +
                                                " deleted successfully!"
                                    }

                                    // log the deletion
                                    // TODO: GitHub's webhooks do provide a way to automatically log all deletions
                                    //      through a webhook, but there is no filter.
                                    //      In the future, we could set something up on Cozy to automatically
                                    //      filter the github webhook to create an action log.
                                    getGitHubLogChannel()?.createEmbed {
                                        val issue = repo.issue

                                        title = "Issue Deleted: ${issue.title}"
                                        color = DISCORD_RED
                                        description = issue.body

                                        field {
                                            val loginAndId = getActorLoginAndId(issue.author!!)

                                            name = "Issue Author"
                                            value = "${loginAndId.first} (${loginAndId.second})"
                                        }

                                        userField(user, "Moderator")

                                        if (arguments.reason != null) {
                                            field {
                                                name = "Reason"
                                                value = arguments.reason!!
                                            }
                                        }
                                    }
                                } else {
                                    respond {
                                        // TODO: need a prettier way to report errors
                                        content = "Could not delete issue due to errors:"
                                        response.errors!!.forEach {
                                            content += "\n" + it.message
                                        }
                                    }
                                }
                            }

                            else -> respond {
                                content = "Could not find issue #${arguments.issue} in repository ${arguments.repo}"
                            }
                        }
                    }
                }
            }
        }
    }

    // Yes, this is stupid.
    private fun getActorLoginAndId(actor: Actor): Pair<String, String> {
        return when (actor) {
            is EnterpriseUserAccount -> Pair(actor.login, actor.id)
            is Organization -> Pair(actor.login, actor.id)
            is Bot -> Pair(actor.login, actor.id)
            is Mannequin -> Pair(actor.login, actor.id)
            is User -> Pair(actor.login, actor.id)
        }
    }

    inner class DeleteIssueArgs : Arguments() {
        val repo by string("repository", "The name of the repository")
        val issue by int("issue", "The number of the issue or pull request to delete")
        val reason by optionalString(
            "reason",
            "A short explanation of why this issue is being deleted"
        )
    }

    inner class LinkUserArgs : Arguments() {
        val login by string("slug", "The slug (username) of the GitHub account to link")
    }
}
