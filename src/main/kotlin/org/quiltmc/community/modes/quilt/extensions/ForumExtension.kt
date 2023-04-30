/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")
@file:OptIn(UnsafeAPI::class, KordUnsafe::class)

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.checks.or
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.ackEphemeral
import com.kotlindiscord.kord.extensions.utils.extraData
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.ChannelType
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.entity.channel.ForumChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import kotlinx.coroutines.delay
import org.quiltmc.community.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ForumExtension : Extension() {
	override val name: String = "forum"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "forum"
			description = "Forum channel management commands"

			check {
				hasBaseModeratorRole(true)

				or {
					hasRole(COMMUNITY_DEVELOPER_ROLE)

					if (passed) {
						event.extraData["isDeveloper"] = true
					}
				}
			}

			unsafeSubCommand(::ForumChannelArgs) {
				name = "create-post"
				description = "Create a Cozy-managed forum post"

				initialResponse = InitialSlashCommandResponse.None

				action {
					val isDeveloper: Boolean = event.extraData.getOrDefault("isDeveloper", false) as Boolean
					val forum = arguments.channel.asChannelOfOrNull<ForumChannel>()

					if (forum == null) {
						ackEphemeral {
							content = "Please provide a forum channel to create a post within."
						}

						return@action
					}

					if (isDeveloper && forum.categoryId != COMMUNITY_DEVELOPER_CATEGORY) {
						ackEphemeral {
							content = "Quilt Developers may only use this command to create posts in the developer " +
								"forum channels."
						}

						return@action
					}

					val form = PostModal()

					this@unsafeSubCommand.componentRegistry.register(form)

					event.interaction.modal(
						form.translateTitle(getLocale(), bundle),
						form.id
					) {
						form.applyToBuilder(this, getLocale(), bundle)
					}

					val interactionResponse = form.awaitCompletion {
						it?.deferEphemeralResponseUnsafe()
					} ?: return@action

					val text = form.postText.value
					val title = form.postTitle.value

					if (text == null || title == null) {
						interactionResponse.createEphemeralFollowup {
							content = "Please provide the post title and text."
						}

						return@action
					}

					val thread = forum.startPublicThread(title) {
						// TODO: Tags?

						message(text)
					}

					interactionResponse.createEphemeralFollowup {
						content = "Post created."
					}

					delay(1.seconds)

					val setupMessage = thread.createMessage {
						content = "Just a moment while we finish setting up..."
					}

					val firstMessage = thread.getFirstMessage()!!

					firstMessage.pin("First message in the forum post")

					setupMessage.edit {
						content = "Adding role: <@&$COMMUNITY_MODERATOR_ROLE>"
					}

					delay(1.seconds)

					setupMessage.edit {
						content = "Adding role: <@&$COMMUNITY_DEVELOPER_ROLE>"
					}

					delay(1.seconds)

					setupMessage.delete("Removing initial setup message")
				}
			}

			unsafeSubCommand(::ForumPostArgs) {
				name = "edit-post"
				description = "Edit an existing Cozy-managed forum post"

				initialResponse = InitialSlashCommandResponse.None

				action {
					val isDeveloper: Boolean = event.extraData.getOrDefault("isDeveloper", false) as Boolean

					val thread = (arguments.post ?: channel)
						.asChannelOfOrNull<ThreadChannel>()

					val parent = thread
						?.parent
						?.asChannelOfOrNull<ForumChannel>()

					if (parent == null) {
						ackEphemeral {
							content = "Please provide a forum thread to edit the first post for, or run this " +
								"command directly within the thread."
						}

						return@action
					}

					if (isDeveloper && parent.categoryId != COMMUNITY_DEVELOPER_CATEGORY) {
						ackEphemeral {
							content = "Quilt Developers may only use this command for threads in the developer " +
								"forum channels."
						}

						return@action
					}

					val firstMessage = thread.getFirstMessage()

					if (firstMessage == null) {
						ackEphemeral {
							content = "Unable to find the first message for this thread - is it a Cozy-managed thread?"
						}

						return@action
					}

					val form = PostModal(thread.name, firstMessage.content)

					this@unsafeSubCommand.componentRegistry.register(form)

					event.interaction.modal(
						form.translateTitle(getLocale(), bundle),
						form.id
					) {
						form.applyToBuilder(this, getLocale(), bundle)
					}

					val interactionResponse = form.awaitCompletion {
						it?.deferEphemeralResponseUnsafe()
					} ?: return@action

					val text = form.postText.value
					val title = form.postTitle.value

					if (text == null || title == null) {
						interactionResponse.createEphemeralFollowup {
							content = "Please provide the post title and text."
						}

						return@action
					}

					if (title != thread.name) {
						thread.edit { name = title }
					}

					if (text != firstMessage.content) {
						firstMessage.edit { content = text }
					}

					interactionResponse.createEphemeralFollowup {
						content = "Post edited."
					}
				}
			}
		}
	}

	inner class ForumChannelArgs : Arguments() {
		val channel by channel {
			name = "channel"
			description = "Forum channel to post in"

			requireChannelType(ChannelType.GuildForum)
		}
	}

	inner class ForumPostArgs : Arguments() {
		val post by optionalChannel {
			name = "post"
			description = "Thread to edit the first post for"

			requireChannelType(ChannelType.PublicGuildThread)
		}
	}

	inner class PostModal(
		private val givenTitle: String? = null,
		private val givenText: String? = null,
	) : ModalForm() {
		override var title: String = "Create/Edit Post"
		override val timeout: Duration = 15.minutes

		val postTitle = lineText {
			label = "Post Text"

			minLength = 1
			required = true

			if (givenTitle == null) {
				placeholder = "Forum post title"
			} else {
				initialValue = givenTitle
			}
		}

		val postText = paragraphText {
			label = "Post Text"

			maxLength = 4000
			minLength = 1
			required = true

			if (givenText == null) {
				placeholder = "Forum post text - supports all Discord Markdown"
			} else {
				initialValue = givenText
			}
		}
	}
}
