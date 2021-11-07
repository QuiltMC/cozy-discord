package org.quiltmc.community.modes.quilt.extensions.github

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.createEmbed
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import org.quiltmc.community.*
import quilt.ghgen.DeleteIssue
import quilt.ghgen.FindIssueId
import quilt.ghgen.findissueid.*
import java.net.URL

class GithubExtension : Extension() {
    override val name = "github"

    private val client = GraphQLKtorClient(
        URL("https://api.github.com/graphql"),
        HttpClient(engineFactory = CIO, block = {
            defaultRequest {
                header("Authorization", "bearer $GITHUB_TOKEN")
            }
        })
    )

    override suspend fun setup() {
        for (guildId in GUILDS) {
            ephemeralSlashCommand() {
                name = "github-issue"
                description = "Manage issues on GitHub"

                guild(guildId)

                when (guildId) {
                    COMMUNITY_GUILD -> check { hasRole(COMMUNITY_MODERATOR_ROLE) }
                    TOOLCHAIN_GUILD -> check { hasRole(TOOLCHAIN_MODERATOR_ROLE) }
                }

                ephemeralSubCommand(::DeleteIssueArgs) {
                    name = "delete"

                    action {
                        val repo = client
                            .execute(FindIssueId(FindIssueId.Variables(arguments.repo, arguments.issue)))
                            .data
                            ?.repository

                        when {
                            repo == null -> respond {
                                content = "Repository ${arguments.repo} not found!"
                            }

                            repo.pullRequest != null -> respond {
                                content = "#${arguments.issue} appears to be a pull request. " +
                                        "Github does not allow deleting pull requests! " +
                                        "Please contact Github Support."
                            }

                            repo.issue != null -> {
                                // try to delete the issue
                                val response = client.execute(DeleteIssue(DeleteIssue.Variables(repo.issue.id)))

                                if (response.errors.isNullOrEmpty()) {
                                    respond {
                                        content = "Issue #${arguments.issue} in repository ${arguments.repo}" +
                                                " deleted successfully!"
                                    }

                                    // log the deletion
                                    // TODO: Github's webhooks do provide a way to automatically log all deletions
                                    //      through a webhook, but there is no filter.
                                    //      In the future, we could set something up on Cozy to automatically
                                    //      filter the github webhook to create an action log.
                                    getGithubLogChannel()?.createEmbed {
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
}
