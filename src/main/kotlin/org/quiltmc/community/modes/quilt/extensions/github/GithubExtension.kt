package org.quiltmc.community.modes.quilt.extensions.github

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import org.quiltmc.community.GITHUB_TOKEN
import org.quiltmc.community.TOOLCHAIN_GUILD
import org.quiltmc.community.TOOLCHAIN_MODERATOR_ROLE
import org.quiltmc.community.github.generated.DeleteIssue
import org.quiltmc.community.github.generated.FindIssueId
import java.net.URL

class GithubExtension : Extension() {
    override val name = "github"

    private val client = GraphQLKtorClient(URL("https://api.github.com/graphql"),
        HttpClient(engineFactory = CIO, block = {
            defaultRequest {
                header("Authorization", "bearer $GITHUB_TOKEN")
            }
        })
    )

    override suspend fun setup() {
        ephemeralSlashCommand() {
            name = "github"
            description = "Perform privileged actions on the Quilt Github organization"

            guild(TOOLCHAIN_GUILD)

            check { hasRole(TOOLCHAIN_MODERATOR_ROLE) }

            ephemeralSubCommand() {
                name = "issue"
                description = "Manage issues on GitHub"

                ephemeralSubCommand(::DeleteIssueArgs) {
                    name = "delete"

                    action {
                        val repo = client.execute(FindIssueId(FindIssueId.Variables(arguments.repo, arguments.issue)))
                            .data?.repository

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
                                respond {
                                    if (response.errors.isNullOrEmpty()) {
                                        // No errors, issue deleted!
                                        content = "Issue #${arguments.issue} in repository ${arguments.repo} deleted" +
                                                " successfully!"
                                    } else {
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

    inner class DeleteIssueArgs : Arguments() {
        val repo by string("repository", "The name of the repository")
        val issue by int("issue", "The number of the issue or pull request to delete")
    }
}
