/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.modhostverify

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.PKMessageCreateEvent
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.getParentMessage
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import dev.kord.common.entity.MessageType
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.*
import org.quiltmc.community.database.collections.OwnedThreadCollection
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.database.entities.OwnedThread
import org.quiltmc.community.modes.quilt.extensions.modhostverify.curseforge.CurseforgeProject
import org.quiltmc.community.modes.quilt.extensions.modhostverify.modrinth.ModrinthProjectVersion
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val MODRINTH_URL_REGEX = Regex("modrinth\\.com/mod/([\\w!@\$()`.+,\"\\-']{3,64})")
private val CURSEFORGE_URL_REGEX = Regex("curseforge\\.com/minecraft/mc-mods/([\\w!@\$()`.+,\"\\-']{3,64})")

// Curseforge takes weekend breaks for project approval, so I've set the time here for 3 days
// So that even the most unlucky people should hopefully be caught by this
val TIME_BETWEEN_PROJECT_STATUS_CHECKS = 60.minutes
const val MAX_RETRY_FOR_CHECKING_PROJECTS = 72
const val POLLING_SECONDS_FOR_PROCESSING = 15L

private val THREAD_DELAY = 3.seconds

private val unitsToProcess = mutableListOf<ModHostingVerificationProcessingUnit>()

class ModHostingVerificationExtension : Extension() {
	override val name: String = "Mod Hosting Verification"

	private val settings: ServerSettingsCollection by inject()

	private val logger = KotlinLogging.logger { }
	private val scheduler = Scheduler()
	private val threads: OwnedThreadCollection by inject()

	private val client = HttpClient {
		install(ContentNegotiation) {
			json(
				Json {
					// Ignore unknown ones, we don't model the *entire* api so this prevents crashing and burning
					ignoreUnknownKeys = true
				}
			)
		}

		// Required by Modrinth, also just good in general
		install(UserAgent) {
			agent = "QuiltMC Cozy Discord Bot"
		}

		// Custom handling for error states that doesn't fit nicely into
		// the standard `HttpResponseValidator` requires this
		expectSuccess = false
	}

	override suspend fun setup() {
		event<PKMessageCreateEvent> {
			check { event.author != null }
			check { isNotBot() }
			check { inQuiltGuild() }
			check { inReleaseChannel() }
			check { failIf(event.message.content.trim().isEmpty()) }
			check {
				failIfNot {
					event.message.type == MessageType.Default ||
							event.message.type == MessageType.Reply
				}
			}

			action {
				handleMessage(event.message, event.author!!)
			}
		}

		scheduler.schedule(
			TIME_BETWEEN_PROJECT_STATUS_CHECKS,
			true,
			"Mod hosting checker",
			POLLING_SECONDS_FOR_PROCESSING,
			true,
			::processUnits
		)
	}

	/**
	 * Takes a list of projects and runs lookups to get states
	 *
	 * If [ProjectStateCollection.anyMarkedAsQuiltCompatible] is true, this method may have
	 * returned early and the other collections are incomplete, which works with our processing logic.
	 */
	private suspend fun getStateOfProjects(projects: List<Project>): ProjectStateCollection {
		// Keep track of any failures so that we can maybe queue up a delayed check later after some more checks
		val projectStates = ProjectStateCollection()

		// Check each project until we find one marked as quilt compatible
		// or collect the others
		projects.forEach {
			val state = when (it.platform) {
				ModPlatform.CURSEFORGE -> checkCurseforge(it.slug)
				ModPlatform.MODRINTH -> checkModrinth(it.slug)
			}

			when (state) {
				ProjectState.MARKED_COMPATIBLE -> {
					projectStates.anyMarkedAsQuiltCompatible = true

					return projectStates
				}

				ProjectState.NOT_MARKED_COMPATIBLE -> projectStates.notMarkedCompatible.add(it)

				ProjectState.NO_RECENT_FILES, ProjectState.NO_PROJECT -> projectStates.missingFiles.add(it)
			}
		}

		return projectStates
	}

	private suspend fun handleMessage(message: Message, author: Member) {
		val content = message.content

		// Locate any mod hosting links in the message
		val modrinthLinks = MODRINTH_URL_REGEX.findAll(content)
		val curseforgeLinks = CURSEFORGE_URL_REGEX.findAll(content)

		// Collect all found projects into 1 list
		// (groups.get will never OOB, only return null)
		val foundProjects = buildList {
			modrinthLinks.forEach {
				val slug = it.groups[1]?.value

				if (slug != null) {
					add(Project(slug, ModPlatform.MODRINTH))
				}
			}

			curseforgeLinks.forEach {
				val slug = it.groups[1]?.value

				if (slug != null) {
					add(Project(slug, ModPlatform.CURSEFORGE))
				}
			}
		}

		val states = getStateOfProjects(foundProjects)

		// Don't have to do anything if we found a project marked as quilt compatible
		if (states.anyMarkedAsQuiltCompatible) {
			return
		}

		// We only found projects not marked as quilt compatible
		if (states.missingFiles.isEmpty() && states.notMarkedCompatible.isNotEmpty()) {
			logger.info {
				"Only found projects not marked as quilt compatible in message ${message.id}, messaging user ${author.id}"
			}

			messageUserAboutAddingCompat(
				author,
				message,
				states.notMarkedCompatible
			)

			return
		}

		// Found some projects that are missing files to check against
		if (states.missingFiles.isNotEmpty()) {
			logger.info { "Missing some files/projects in message ${message.id}, adding to queue to attempt again later" }

			// Queue for future processing
			unitsToProcess.add(
				ModHostingVerificationProcessingUnit(
					author,
					message,
					states.missingFiles.toMutableList(),
					MAX_RETRY_FOR_CHECKING_PROJECTS
				)
			)
		}
	}

	/**
	 * Makes a call to the modrinth API for the project files, checks each recent one to see
	 * if any are marked as quilt and returns the proper status.
	 */
	private suspend fun checkModrinth(projectSlug: String): ProjectState {
		// There's an optional `loaders` filter here which I've opted not to use
		// as if we do the processing ourselves, we can differentiate between
		// having no versions, and no versions marked as quilt compatible
		val queryUrl = "https://api.modrinth.com/v2/project/$projectSlug/version"

		val projectVersionsResponse: HttpResponse = client.get(queryUrl)

		// Doesn't exist yet, or we cant see it
		if (!canHandleResponse(projectVersionsResponse)) {
			return ProjectState.NO_PROJECT
		}

		// Convert the raw response into a list of our version objects
		val versionsList: List<ModrinthProjectVersion> = projectVersionsResponse.body()
		val oneWeekAgo = Instant.now().minusMillis(5.days.inWholeMilliseconds)
		val recentFiles = versionsList.filter {
			Instant.parse(it.datePublished).isAfter(oneWeekAgo)
		}

		// No files at all, check later
		if (recentFiles.isEmpty()) {
			return ProjectState.NO_RECENT_FILES
		}

		// Check if any files are marked as quilt compatible and return the result
		return if (recentFiles.any { it.loaders.contains("quilt") }) {
			ProjectState.MARKED_COMPATIBLE
		} else {
			ProjectState.NOT_MARKED_COMPATIBLE
		}
	}

	/**
	 * Makes a proxied call to the curseforge API for the project files, checks each to
	 * see if any of the recent ones are marked as quilt compatible
	 * and returns the proper status.
	 */
	private suspend fun checkCurseforge(projectSlug: String): ProjectState {
		val queryUrl = "https://api.cfwidget.com/minecraft/mc-mods/$projectSlug"

		val projectVersionsResponse: HttpResponse = client.get(queryUrl)

		// Doesn't exist yet, or we cant see it
		if (!canHandleResponse(projectVersionsResponse)) {
			return ProjectState.NO_PROJECT
		}

		// Convert the raw response into a project
		val project: CurseforgeProject = projectVersionsResponse.body()
		val versionsList = project.files
		val oneWeekAgo = Instant.now().minusMillis(5.days.inWholeMilliseconds)
		val recentFiles = versionsList.filter {
			Instant.parse(it.datePublished).isAfter(oneWeekAgo)
		}

		// No files at all, check later
		if (recentFiles.isEmpty()) {
			return ProjectState.NO_RECENT_FILES
		}

		// Check if any files are marked as quilt compatible and return the result
		return if (recentFiles.any { it.versions.contains("Quilt") }) {
			ProjectState.MARKED_COMPATIBLE
		} else {
			ProjectState.NOT_MARKED_COMPATIBLE
		}
	}

	private suspend fun canHandleResponse(response: HttpResponse): Boolean {
		if (response.status == HttpStatusCode.NotFound) {
			return false
		}

		// Throw a proper error if the response isn't OK
		if (response.status != HttpStatusCode.OK) {
			val cachedResponseText = response.bodyAsText()

			// Taken from ktor code, this corresponds to the relevant error type for each http error code
			@Suppress("MagicNumber")
			throw when (response.status.value) {
				in 300..399 -> RedirectResponseException(response, cachedResponseText)
				in 400..499 -> ClientRequestException(response, cachedResponseText)
				in 500..599 -> ServerResponseException(response, cachedResponseText)
				else -> ResponseException(response, cachedResponseText)
			}
		}

		return true
	}

	/**
	 * Tries to send the user a DM, and if failing that, falls back to a thread message.
	 */
	private suspend fun messageUserAboutAddingCompat(
		author: User,
		message: Message,
		projects: List<Project>
	) {
		val sentDm = author.dm {
			attachQuiltCompatEmbed(author, message, projects, false)
		}

		val reportChannelId = settings.get(message.getGuild().id)?.cozyLogChannel
		val messageChannel = message.channel.asChannelOf<TextChannel>()

		if (reportChannelId != null) {
			kord.getChannelOf<TextChannel>(reportChannelId)?.createEmbed {
				title = "Mod release not marked as quilt compatible!"
				color = DISCORD_RED

				field {
					name = "Author:"
					value = "${author.mention} (`${author.username}#${author.discriminator}`)"
				}

				field {
					name = "Message:"
					value = "https://discord.com/channels/${messageChannel.guildId}/${messageChannel.id}/${message.id}"
				}

				field {
					name = "Contact:"

					value = if (sentDm == null) {
						"Failed to DM, falling back to thread"
					} else {
						"User notified in DMs"
					}
				}
			}
		}

		logger.info {
			"Mod release by " +
					"${author.id} (${author.username}#${author.discriminator}) at " +
					"https://discord.com/channels/${messageChannel.guildId}/${messageChannel.id}/${message.id} " +
					"is not quilt compatible, " +
					if (sentDm == null) { "failed to DM, falling back to thread" } else { "user notified in DMs" }
		}

		if (sentDm == null) {
			fallbackToThreadForCompatNotice(author, message, projects)
		}
	}

	private suspend fun fallbackToThreadForCompatNotice(author: User, message: Message, projects: List<Project>) {
		val channel = message.channel.asChannelOf<TextChannel>()

		val possibleExistingThread = channel.activeThreads.filter {
			!it.isPrivate && !it.isLocked && it.getParentMessage() == message
		}.firstOrNull()

		if (possibleExistingThread != null) {
			possibleExistingThread.createMessage { attachQuiltCompatEmbed(author, message, projects, true) }
		} else {
			val thread = channel.startPublicThreadWithMessage(message.id, message.contentToThreadName(channel.name))

			threads.set(
				OwnedThread(thread.id, author.id, message.getGuild().id)
			)

			val threadMessage = thread.createMessage {
				content = "Oh hey, that's a nice post you've got there! Let me just get the mods in on " +
						"this..."
			}

			threadMessage.pin("First message in the thread.")

			thread.withTyping {
				delay(THREAD_DELAY)
			}

			val guild = message.getGuild()
			val role = when (guild.id) {
				COMMUNITY_GUILD -> guild.getRole(COMMUNITY_MODERATOR_ROLE)
				TOOLCHAIN_GUILD -> guild.getRole(TOOLCHAIN_MODERATOR_ROLE)

				else -> return
			}

			threadMessage.edit {
				content = "Hey, ${role.mention}, you've gotta check this showcase out!"
			}

			thread.withTyping {
				delay(THREAD_DELAY)
			}

			threadMessage.edit {
				content = "Welcome to your new thread, ${author.mention}! This message is at the " +
						"start of the thread. Remember, you're welcome to use the `/thread` commands to manage " +
						"your thread as needed.\n\n" +

						"We recommend using `/thread rename` to give your thread a more meaningful title if the " +
						"generated one isn't good enough!"
			}

			thread.createMessage { attachQuiltCompatEmbed(author, message, projects, true) }
		}
	}

	private suspend fun MessageCreateBuilder.attachQuiltCompatEmbed(
		author: User,
		message: Message,
		projects: List<Project>,
		failedToDmUser: Boolean
	) {
		val channel = message.channel.asChannelOf<TextChannel>()

		if (failedToDmUser) {
			content = author.mention
		}

		embed {
			title = "Are your projects marked as quilt compatible?"
			color = DISCORD_RED

			field {
				value = if (failedToDmUser) {
					"While checking out your post here it seems like the linked mods " +
							"aren't marked as quilt compatible, please update the ones meant to be."
				} else {
					"While checking out your post in ${channel.mention} (`#${channel.name}`) " +
							"[here](https://discord.com/channels/${channel.guildId}/${channel.id}/${message.id}) " +
							"it seems like the linked mods aren't marked as quilt compatible, " +
							"please update the ones meant to be."
				}
			}

			field {
				value = buildString {
					projects.forEach {
						append("**- ${it.slug}** (${it.platform.name.lowercase()})\n")
					}
				}
			}
		}
	}

	private suspend fun processUnits() {
		val toRequeue = mutableListOf<ModHostingVerificationProcessingUnit>()

		val failed = mutableListOf<ModHostingVerificationProcessingUnit>()

		unitsToProcess.forEach { processingUnit ->
			val states = getStateOfProjects(processingUnit.missingFiles)

			// Found a project marked compatible, don't do anything
			if (states.anyMarkedAsQuiltCompatible) {
				return@forEach
			}

			// We only found projects not marked as quilt compatible
			if (states.missingFiles.isEmpty() && states.notMarkedCompatible.isNotEmpty()) {
				failed.add(processingUnit)

				messageUserAboutAddingCompat(
					processingUnit.author,
					processingUnit.message,
					states.notMarkedCompatible
				)

				return@forEach
			}

			// Found some projects that are missing files to check against
			if (states.missingFiles.isNotEmpty() && processingUnit.remainingAttempts - 1 >= 0) {
				// Requeue for future processing
				toRequeue.add(processingUnit.copy(remainingAttempts = processingUnit.remainingAttempts - 1))
			}
		}

		unitsToProcess.clear()
		unitsToProcess.addAll(toRequeue)

		logger.info {
			buildString {
				if (failed.isNotEmpty()) {
					appendLine("Failed projects:")
					failed.forEach {
						appendLine(" - Message ${it.message.id} Author ${it.author.id} Projects ${it.missingFiles}")
					}

					appendLine("Requeued projects:")
					toRequeue.forEach {
						appendLine(" - Message ${it.message.id} Author ${it.author.id} Projects ${it.missingFiles}")
					}
				}
			}
		}
	}
}
