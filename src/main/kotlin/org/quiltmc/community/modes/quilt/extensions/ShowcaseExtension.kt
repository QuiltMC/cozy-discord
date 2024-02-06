/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(ExperimentalTime::class)

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.checks.isNotInThread
import com.kotlindiscord.kord.extensions.checks.notHasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.group
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.PKMessageCreateEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.storage.StorageType
import com.kotlindiscord.kord.extensions.storage.StorageUnit
import com.kotlindiscord.kord.extensions.utils.authorId
import com.kotlindiscord.kord.extensions.utils.capitalizeWords
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.CategorizableChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.message.ReactionAddEvent
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.codebox.homoglyph.HomoglyphBuilder
import org.koin.core.component.inject
import org.quiltmc.community.*
import org.quiltmc.community.database.collections.OwnedThreadCollection
import org.quiltmc.community.database.entities.OwnedThread
import org.quiltmc.community.modes.quilt.extensions.storage.BannedReactions
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private val THREAD_DELAY = 3.seconds

private val CHANNEL_REGEX = "<#(\\d)+>".toRegex()

class ShowcaseExtension : Extension() {
	override val name: String = "showcase"

	private val bannedReactionStorage = StorageUnit(
		StorageType.Config,
		"cozy-showcase",
		"banned-reactions",
		BannedReactions::class
	)

	private val threads: OwnedThreadCollection by inject()
	private val latestReactionWarnings: MutableMap<Snowflake, Instant> = mutableMapOf()
	private val homoglyphs = HomoglyphBuilder.build()

	override suspend fun setup() {
		event<PKMessageCreateEvent> {
			check {
				failIfNot {
					event.message.type == MessageType.Default ||
							event.message.type == MessageType.Reply
				}
			}

			check { failIf(event.message.data.authorId == event.kord.selfId) }
			check { failIf(event.message.content.trim().isEmpty()) }
			check { failIf(event.message.interaction != null) }

			// TODO: switch back to inChannel once the bug is fixed
			// Currently, due to a lack of support for the pluralkit events, inChannel
			// will always return false.
			check { failIf(event.message.channelId != GALLERY_CHANNEL) }

			check {
				// Don't do anything if the message mentions a thread in the same channel.
				val threads = CHANNEL_REGEX.findAll(event.message.content)
					.toList()
					.map { Snowflake(it.groupValues[1]) }
					.mapNotNull { kord.getChannelOf<TextChannelThread>(it) }
					.filter { it.parentId == GALLERY_CHANNEL }
					.toList()

				failIf(threads.isNotEmpty())
			}

			action {
				// TODO: This sort of thing *needs* to be factored out into a broader threads manager

				val guild = event.message.getGuild()

				val role = when (guild.id) {
					COMMUNITY_GUILD -> guild.getRole(COMMUNITY_MODERATOR_ROLE)
					TOOLCHAIN_GUILD -> guild.getRole(TOOLCHAIN_MODERATOR_ROLE)

					else -> return@action
				}

				val authorId = event.message.author?.id ?: (event as ProxiedMessageCreateEvent).pkMessage.sender
				val channel = event.message.channel.asChannelOf<TextChannel>()

				val title = event.message.contentToThreadName("Gallery")

				val thread = channel.startPublicThreadWithMessage(
					event.message.id, title
				) { reason = "Showcase thread created for ${event.message.author?.tag}" }

				threads.set(
					OwnedThread(thread.id, authorId, guild.id)
				)

				val message = thread.createMessage {
					content = "Oh hey, that's a nice gallery post you've got there! Let me just get the mods in on " +
							"this sweet showcase..."
				}

				message.pin("First message in the thread.")

				thread.withTyping {
					delay(THREAD_DELAY)
				}

				message.edit {
					content = "Hey, ${role.mention}, you've gotta check this showcase out!"
				}

				thread.withTyping {
					delay(THREAD_DELAY)
				}

				message.edit {
					content = "Welcome to your new gallery thread, <@$authorId>! This message is at the " +
							"start of the thread. Remember, you're welcome to use the `/thread` commands to manage " +
							"your thread as needed.\n\n" +

							"We recommend using `/thread rename` to give your thread a more meaningful title if the " +
							"generated one isn't good enough!\n\n" +

							"**Note:** To avoid filling up the sidebar, this thread has been archived on creation. " +
							"Feel free to send a message or rename it to unarchive it!"
				}

				thread.edit {
					archived = true
					reason = "Gallery thread archived on creation."
				}
			}
		}

		event<ReactionAddEvent> {
			check { anyGuild() }
			check { isNotInThread() }
			check { notHasPermission(Permission.ManageGuild) }

			action {
				val storage = bannedReactionStorage.withGuild(event.guildId!!)

				val bannedReactions = storage.get()
					?: return@action

				val channel = event.channel.asChannelOfOrNull<CategorizableChannel>()
					?: return@action

				if (channel.categoryId !in bannedReactions.categories) {
					return@action
				}

				val matches = bannedReactions.reactionNames
					.filter { homoglyphs.search(event.emoji.name, it).isNotEmpty() }

				if (matches.isEmpty()) {
					return@action
				}

				this.event.message.deleteReaction(this.event.emoji)

				if (
					latestReactionWarnings.getOrDefault(this.event.user.id, Instant.DISTANT_PAST) >
					Clock.System.now().minus(5.minutes)
				) {
					return@action
				}

				latestReactionWarnings[this.event.user.id] = Clock.System.now()

				this.event.user.asUser().dm {
					content =
						"Your reaction (`:${event.emoji.name}:`) has been removed. We remove certain reactions from " +
								"messages in the **${channel.category?.asChannel()?.name?.capitalizeWords()} " +
								"Category** because we don't want developers to feel pressured to upload their " +
								"projects to other services, or make them open-source if they don't want to." +
								"\n\n" +
								"It's up to the individual developer to decide how they'd like to license and " +
								"distribute their project, and we want everyone to feel like they can safely share " +
								"their creations without being harassed or pressured by users that don't agree with " +
								"their choices."
				}
			}
		}

		ephemeralSlashCommand {
			name = "showcase"
			description = "Showcase channel configuration commands"

			allowInDms = false

			check { anyGuild() }
			check { hasPermission(Permission.ManageGuild) }

			group("filtered-categories") {
				description = "Manage categories that reactions should be filtered within."

				ephemeralSubCommand(::CategoryArgs) {
					name = "add"
					description = "Add a category to filter reactions within."

					action {
						val storage = bannedReactionStorage.withGuild(guild!!)

						val bannedReactions = storage
							.withGuild(guild!!)
							.get()
							?: BannedReactions()

						val result = bannedReactions.categories.add(arguments.category.id)

						if (!result) {
							respond {
								content = "Already filtering reactions in category: ${arguments.category.mention}"
							}

							return@action
						}

						storage.save(bannedReactions)

						respond {
							content = "Filtering reactions in category: ${arguments.category.mention}"
						}
					}
				}

				ephemeralSubCommand(::CategoryArgs) {
					name = "remove"
					description = "Remove a filtered category."

					action {
						val storage = bannedReactionStorage.withGuild(guild!!)

						val bannedReactions = storage
							.withGuild(guild!!)
							.get()
							?: BannedReactions()

						val result = bannedReactions.categories.remove(arguments.category.id)

						if (!result) {
							respond {
								content = "Already not filtering reactions in category: ${arguments.category.mention}"
							}

							return@action
						}

						storage.save(bannedReactions)

						respond {
							content = "No longer filtering reactions in category: ${arguments.category.mention}"
						}
					}
				}

				ephemeralSubCommand {
					name = "list"
					description = "List all filtered categories."

					action {
						val storage = bannedReactionStorage.withGuild(guild!!)

						val bannedReactions = storage
							.withGuild(guild!!)
							.get()
							?: BannedReactions()

						if (bannedReactions.categories.isEmpty()) {
							respond {
								content = "No categories have been configured for reaction filtering."
							}

							return@action
						}

						respond {
							content = buildString {
								appendLine("Filtering reactions in the following categories:")
								appendLine()

								bannedReactions.categories.forEach {
									appendLine("**»** `$it` | <#$it>")
								}
							}
						}
					}
				}
			}

			group("filtered-reactions") {
				description = "Manage reactions to filter out of the configured categories."

				ephemeralSubCommand(::ReactionNameArgs) {
					name = "add"
					description = "Add a partial reaction name to filter."

					action {
						val storage = bannedReactionStorage.withGuild(guild!!)

						val bannedReactions = storage
							.withGuild(guild!!)
							.get()
							?: BannedReactions()

						val result = bannedReactions.reactionNames.add(arguments.partialName)

						if (!result) {
							respond {
								content = "Already filtering reactions containing the text: ${arguments.partialName}"
							}

							return@action
						}

						storage.save(bannedReactions)

						respond {
							content = "Filtering reactions containing the text: `${arguments.partialName}`"
						}
					}
				}

				ephemeralSubCommand(::ReactionNameArgs) {
					name = "remove"
					description = "Remove a filtered partial reaction name."

					action {
						val storage = bannedReactionStorage.withGuild(guild!!)

						val bannedReactions = storage
							.withGuild(guild!!)
							.get()
							?: BannedReactions()

						val result = bannedReactions.reactionNames.remove(arguments.partialName)

						if (!result) {
							respond {
								content = "Already not filtering reactions containing the text: " +
										arguments.partialName
							}

							return@action
						}

						storage.save(bannedReactions)

						respond {
							content = "No longer filtering reactions containing the text: `${arguments.partialName}`"
						}
					}
				}

				ephemeralSubCommand {
					name = "list"
					description = "List all filtered partial reaction names."

					action {
						val storage = bannedReactionStorage.withGuild(guild!!)

						val bannedReactions = storage
							.withGuild(guild!!)
							.get()
							?: BannedReactions()

						if (bannedReactions.reactionNames.isEmpty()) {
							respond {
								content = "No partial reaction names have been configured for reaction filtering."
							}

							return@action
						}

						respond {
							content = buildString {
								appendLine("Filtering reactions containing the following text:")
								appendLine()

								bannedReactions.reactionNames.forEach {
									appendLine("**»** `$it`")
								}
							}
						}
					}
				}
			}
		}
	}

	inner class CategoryArgs : Arguments() {
		val category by channel {
			name = "category"
			description = "Category to use for this command."

			requireChannelType(ChannelType.GuildCategory)
		}
	}

	inner class ReactionNameArgs : Arguments() {
		val partialName by string {
			name = "partial-name"
			description = "Partial emoji name to filter by."
		}
	}
}
