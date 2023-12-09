/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")

package org.quiltmc.community.modes.quilt.extensions.github

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.quiltmc.community.*
import org.quiltmc.community.modes.quilt.extensions.github.types.GitHubSimpleUser
import quilt.ghgen.DeleteIssue
import quilt.ghgen.FindIssueId
import quilt.ghgen.findissueid.*
import java.net.URL

private const val USERS_PER_PAGE = 10
private const val BLOCKS_URL = "https://api.github.com/orgs/{ORG}/blocks"

class GithubExtension : Extension() {
	override val name = "github"

	val logger = KotlinLogging.logger { }

	private val graphQlClient = GraphQLKtorClient(
		URL("https://api.github.com/graphql"),

		HttpClient(engineFactory = CIO) {
			defaultRequest {
				header("Authorization", "bearer $GITHUB_TOKEN")
			}
		}
	)

	private val client = HttpClient(engineFactory = CIO) {
		defaultRequest {
			header("Authorization", "token $GITHUB_TOKEN")
		}

		install(ContentNegotiation) {
			json(
				Json {
					ignoreUnknownKeys = true
				}
			)
		}

		expectSuccess = false
	}

	private suspend fun getOrgBlocks(org: String): List<GitHubSimpleUser> {
		val response = client.get(BLOCKS_URL.replace("{ORG}", org))

		logger.info { "GET $BLOCKS_URL -> ${response.status.value}; {ORG} = $org" }

		if (response.status.value >= 400) {
			val text = response.bodyAsText()

			logger.error { "Response content:\n\n$text" }
		}

		return response.body()
	}

	@Suppress("MagicNumber")
	private suspend fun addOrgBlock(org: String, user: String): Boolean {
		val response = client.put(BLOCKS_URL.replace("{ORG}", org) + "/$user")

		logger.info { "PUT $BLOCKS_URL/{USER} -> ${response.status.value}; {ORG} = $org, {USER} = $user" }

		if (response.status.value >= 400) {
			val text = response.bodyAsText()

			logger.error { "Response content:\n\n$text" }
		}

		return response.status.value == 204
	}

	@Suppress("MagicNumber")
	private suspend fun removeOrgBlock(org: String, user: String): Boolean {
		val response = client.delete(BLOCKS_URL.replace("{ORG}", org) + "/$user")

		logger.info { "DELETE $BLOCKS_URL/{USER} -> ${response.status.value}; {ORG} = $org, {USER} = $user" }

		if (response.status.value >= 400) {
			val text = response.bodyAsText()

			logger.error { "Response content:\n\n$text" }
		}

		return response.status.value == 204
	}

	override suspend fun setup() {
		for (guildId in GUILDS) {
			ephemeralSlashCommand {
				name = "github"
				description = "GitHub management commands"

				allowInDms = false

				check { hasBaseModeratorRole(false) }

				guild(guildId)
				requirePermission(Permission.BanMembers)

				group("blocks") {
					description = "Organization block management commands"

					ephemeralSubCommand(::OrgArgs) {
						name = "list"
						description = "List all users that are blocked from the organization"

						@Suppress("TooGenericExceptionCaught")
						action {
							val blocks = try {
								getOrgBlocks(arguments.org)
							} catch (e: Exception) {
								logger.error(e) { "Failed to retrieve blocked users for ${arguments.org}" }

								respond {
									content = "Failed to retrieve blocked users for `${arguments.org}` - does the " +
											"bot have permission?"
								}

								return@action
							}

							if (blocks.isEmpty()) {
								respond {
									content = "No users have been blocked from `${arguments.org}`"
								}
							} else {
								editingPaginator {
									blocks.sortedBy { it.login }
										.chunked(USERS_PER_PAGE).forEach {
											page {
												title = "Blocked users: ${arguments.org}"

												description = it.joinToString("\n") {
													"🚫 [${it.login}](${it.url})"
												}
											}
										}
								}.send()
							}
						}
					}

					ephemeralSubCommand(::BlockUserArgs) {
						name = "add"
						description = "Block a user from the organization"

						action {
							val response = addOrgBlock(arguments.org, arguments.username)

							if (!response) {
								respond {
									content = "User `${arguments.username}` is already blocked from `${arguments.org}`"
								}

								return@action
							}

							getGithubLogChannel()?.createEmbed {
								title = "User blocked from ${arguments.org}: ${arguments.username}"
								color = DISCORD_RED

								userField(user, "Moderator")
							}

							respond {
								content = "User `${arguments.username}` is now blocked from `${arguments.org}`"
							}
						}
					}

					ephemeralSubCommand(::BlockUserArgs) {
						name = "remove"
						description = "Unblock a user from the organization"

						action {
							val response = removeOrgBlock(arguments.org, arguments.username)

							if (!response) {
								respond {
									content = "User `${arguments.username}` is not blocked from `${arguments.org}`"
								}

								return@action
							}

							getGithubLogChannel()?.createEmbed {
								title = "User unblocked from ${arguments.org}: ${arguments.username}"
								color = DISCORD_GREEN

								userField(user, "Moderator")
							}

							respond {
								content = "User `${arguments.username}` is now unblocked from `${arguments.org}`"
							}
						}
					}
				}

				group("issues") {
					description = "Issue management commands"

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
											"Github does not allow deleting pull requests! " +
											"Please contact Github Support."
								}

								repo.issue != null -> {
									// try to delete the issue
									val response =
										graphQlClient.execute(DeleteIssue(DeleteIssue.Variables(repo.issue.id)))

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
												value = "${loginAndId?.first} " +
														"(${loginAndId?.second ?: "Unable to get actor login and id"})"
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

		logger.info { "Set up GitHub extension for ${GUILDS.size} guilds." }
	}

	// Yes, this is stupid.
	private fun getActorLoginAndId(actor: Actor): Pair<String, String>? {
		return when (actor) {
			is EnterpriseUserAccount -> Pair(actor.login, actor.id)
			is Organization -> Pair(actor.login, actor.id)
			is Bot -> Pair(actor.login, actor.id)
			is Mannequin -> Pair(actor.login, actor.id)
			is User -> Pair(actor.login, actor.id)
			else -> null
		}
	}

	inner class BlockUserArgs : Arguments() {
		val username by string {
			name = "username"
			description = "Username to block/unblock"
		}

		val org by defaultingString {
			name = "organization"
			description = "Organization to block/unblock from"

			defaultValue = "QuiltMC"
		}
	}

	inner class OrgArgs : Arguments() {
		val org by defaultingString {
			name = "organization"
			description = "Organization to operate on"

			defaultValue = "QuiltMC"
		}
	}

	inner class DeleteIssueArgs : Arguments() {
		val repo by string {
			name = "repository"
			description = "The name of the repository"
		}

		val issue by int {
			name = "issue"
			description = "The number of the issue or pull request to delete"
		}

		val reason by optionalString {
			name = "reason"
			description = "A short explanation of why this issue is being deleted"
		}
	}
}
