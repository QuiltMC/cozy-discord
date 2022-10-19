/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("MagicNumber")

package org.quiltmc.community.cozy.modules.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.annotations.DoNotChain
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.duration
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.PKMessageCreateEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageCreateEvent
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.deleteIgnoringNotFound
import com.kotlindiscord.kord.extensions.utils.removeTimeout
import com.kotlindiscord.kord.extensions.utils.timeout
import dev.kord.common.Color
import dev.kord.core.behavior.channel.*
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.DateTimePeriod
import org.quiltmc.community.cozy.modules.moderation.config.ModerationConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public val MAXIMUM_SLOWMODE_DURATION: DateTimePeriod = DateTimePeriod(hours = 6)
public const val MAX_TIMEOUT_SECS: Int = 60 * 60 * 24 * 28
public val MOD_MODE_DELAY: Duration = 25.seconds
public val MOD_COLOUR: Color = Color(0xe68675)

/**
 * Moderation, extension, provides different moderation related tools.
 *
 * Currently includes:
 * - Slowmode
 * - Timeout
 * - Force verify
 * - Mod mode
 */
public class ModerationExtension(
	private val config: ModerationConfig
) : Extension() {
	override val name: String = ModerationPlugin.id

	@OptIn(DoNotChain::class)
	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "mod-mode"
			description = "Repost your next message with official styling; waits for 20s"

			allowInDms = false

			config.getCommandChecks().forEach(::check)

			action {
				respond {
					content = "Waiting for you to send a message for the next 20 seconds. Say " +
							"`cancel` to cancel your message.\n\n" +

							"**Note:** This will repost your next message in any channel, not this one " +
							"specifically. Additionally, **uploaded files will be lost!**"
				}

				val event = channel.withTyping {
					bot.waitFor<PKMessageCreateEvent>(MOD_MODE_DELAY) {
						this.author != null &&
								this.author!!.id == this@action.member!!.id &&

								this.guildId != null &&
								this.guildId == this@action.guild!!.id
					}
				}

				if (event == null) {
					respond {
						content = "It looks like you're taking too long to send a message, so I've stopped " +
								"waiting for one."
					}

					return@action
				}

				event.message.deleteIgnoringNotFound()

				if (event.message.content == "cancel") {
					respond {
						content = "Alright, cancelling message."
					}

					return@action
				}

				event.message.channel.createMessage {
					embed {
						color = MOD_COLOUR
						description = event.message.content

						footer {
							text = "Moderator message"
							icon = "https://github.com/QuiltMC/art/raw/master/emoji/lil-pineapple/rendered/" +
									"lil-pineapple.png"
						}

						author {
							name = if (event is ProxiedMessageCreateEvent && event.pkMessage.member != null) {
								buildString {
									append(event.pkMessage.member?.displayName ?: event.pkMessage.member?.name)
									append(" ")

									if (event.pkMessage.system != null) {
										append(event.pkMessage.system?.tag ?: "(${event.pkMessage.system?.id})")
									} else {
										append("(${event.author.displayName})")
									}
								}
							} else {
								event.author!!.displayName
							}

							icon = if (event is ProxiedMessageCreateEvent && event.pkMessage.member != null) {
								event.pkMessage.member?.avatarUrl
									?: event.author.memberAvatar?.url
									?: event.author.avatar?.url
							} else {
								event.author?.memberAvatar?.url
									?: event.author?.avatar?.url
							}
						}
					}
				}
			}
		}

		ephemeralSlashCommand(::TimeoutArguments) {
			name = "timeout"
			description = "Remove or apply a timeout to a user"

			allowInDms = false

			config.getCommandChecks().forEach(::check)

			action {
				val reason = arguments.reason ?: "No reason given"

				if (arguments.duration != null) {
					arguments.user.timeout(
						arguments.duration!!,
						reason = "Timed out by ${user.asUser().tag}: $reason"
					)

					respond {
						content = "Timeout applied."
					}
				} else {
					arguments.user.removeTimeout(
						reason = "Timeout removed by ${user.asUser().tag}: $reason"
					)

					respond {
						content = "Timeout removed."
					}
				}
			}
		}

		ephemeralSlashCommand {
			name = "slowmode"
			description = "Manage slowmode of the current channel or thread"

			allowInDms = false

			check { anyGuild() }

			config.getCommandChecks().forEach(::check)

			ephemeralSubCommand {
				name = "get"
				description = "Get the slowmode of the channel or thread"

				action {
					respond {
						content = "Slowmode is currently " +
								"${channel.asChannelOf<GuildMessageChannel>().data.rateLimitPerUser.value ?: 0} " +
								"second(s)."
					}
				}
			}

			ephemeralSubCommand {
				name = "reset"
				description = "Reset the slowmode of the channel or thread back to 0"

				action {
					val channel = this.channel.asChannel() as? TextChannel
					val thread = this.channel.asChannel() as? ThreadChannel

					thread?.edit {
						rateLimitPerUser = Duration.ZERO
					}

					channel?.edit {
						rateLimitPerUser = Duration.ZERO
					}

					respond {
						content = "Slowmode reset."
					}
				}
			}

			ephemeralSubCommand(::SlowmodeEditArguments) {
				name = "set"
				description = "Set the slowmode of the channel or thread"

				action {
					val channel = this.channel.asChannel() as? TextChannel
					val thread = this.channel.asChannel() as? ThreadChannel

					thread?.edit {
						rateLimitPerUser = arguments.duration.toTotalSeconds().seconds
					}

					channel?.edit {
						rateLimitPerUser = arguments.duration.toTotalSeconds().seconds
					}

					config.getLoggingChannelOrNull(guild!!.asGuild())?.createEmbed {
						title = "Slowmode changed"
						description = "Set to ${arguments.duration.toTotalSeconds()} second(s)."
						color = DISCORD_BLURPLE

						field {
							inline = true
							name = "Channel"
							value = channel?.mention ?: thread!!.mention
						}

						field {
							inline = true
							name = "User"
							value = user.mention
						}
					}

					respond {
						content = "Slowmode set to ${arguments.duration.toTotalSeconds()} second(s)."
					}
				}
			}
		}

		ephemeralSlashCommand(::ForceVerifyArguments) {
			name = "force-verify"
			description = "Make a user bypass Discord's verification process"

			allowInDms = false

			check { anyGuild() }

			config.getCommandChecks().forEach(::check)

			action {
				val member = guild!!.getMemberOrNull(arguments.user.id)

				if (member == null) {
					respond {
						content = "User is not in this guild."
					}
				} else {
					member.addRole(
						config.getTemporaryRole(guild!!.asGuild()).id,
						"Force verified by ${user.asUser().tag}"
					)
					member.removeRole(config.getTemporaryRole(guild!!.asGuild()).id)

					config.getLoggingChannelOrNull(guild!!.asGuild())?.createEmbed {
						title = "User force verified"
						color = DISCORD_BLURPLE

						field {
							inline = true
							name = "Moderator"
							value = "${user.asUser().tag} (${user.mention})"
						}

						field {
							inline = true
							name = "User"
							value = "${member.tag} (${member.mention})"
						}
					}

					respond {
						content = "User ${member.mention} has been force verified."
					}
				}
			}
		}
	}

	public inner class SlowmodeEditArguments : Arguments() {
		public val duration: DateTimePeriod by duration {
			name = "duration"
			description = "The new duration of the slowmode"

			validate {
				failIf(
					"Slowmode cannot be longer than ${MAXIMUM_SLOWMODE_DURATION.hours} hours"
				) { value > MAXIMUM_SLOWMODE_DURATION }
			}
		}
	}

	public inner class TimeoutArguments : Arguments() {
		public val user: Member by member {
			name = "member"
			description = "Member to apply a timeout to"
		}

		public val duration: DateTimePeriod? by optionalDuration {
			name = "duration"
			description = "How long to time out for, from now"

			validate {
				failIf(
					"Timeouts must be for less than 28 days"
				) { value != null && value!!.toTotalSeconds() >= MAX_TIMEOUT_SECS }
			}
		}

		public val reason: String? by optionalString {
			name = "reason"
			description = "Optional reason for applying this timeout"
			maxLength = 50
		}
	}

	public inner class ForceVerifyArguments : Arguments() {
		public val user: Member by member {
			name = "member"
			description = "Member to verify"
		}
	}
}

public suspend inline fun <reified T : Any> ExtensibleBot.waitFor(
	timeout: Duration? = null,
	noinline condition: (suspend T.() -> Boolean) = { true }
): T? = if (timeout == null) {
	events.filterIsInstance<T>().firstOrNull(condition)
} else {
	withTimeoutOrNull(timeout) {
		events.filterIsInstance<T>().firstOrNull(condition)
	}
}
