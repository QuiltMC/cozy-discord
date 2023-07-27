/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.ama

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.components.ComponentContainer
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.koin.dsl.bind
import org.quiltmc.community.cozy.modules.ama.data.AmaData
import org.quiltmc.community.cozy.modules.ama.enums.QuestionStatusFlag

public fun ExtensibleBotBuilder.ExtensionsBuilder.extAma(data: AmaData) {
	loadModule { single { data } bind AmaData::class }

	add(::AmaExtension)
}

public val User.uniqueName: String
	get() = if (this.discriminator == "0") {
		// Migrated to new username system
		"@${this.username}"
	} else {
		// Still using name#discrim system
		this.tag
	}

public fun EmbedBuilder.questionEmbed(
	interactionUser: User,
	question: String?,
	flag: QuestionStatusFlag,
	pkMember: PKMember?,
	flaggedBy: User? = null,
	claimedOrSkippedBy: User? = null,
	viaStage: Boolean? = null
) {
	if (pkMember != null) {
		author {
			name = "${pkMember.displayName ?: pkMember.name} (${interactionUser.id})"
			icon = pkMember.avatarUrl ?: pkMember.webhookAvatarUrl
		}
	} else {
		author {
			name = "${interactionUser.uniqueName} (${interactionUser.id})"
			icon = interactionUser.avatar?.cdnUrl?.toUrl()
		}
	}

	description = question
	color = when (flag) {
		QuestionStatusFlag.ACCEPTED, QuestionStatusFlag.CLAIMED -> DISCORD_GREEN
		QuestionStatusFlag.DENIED, QuestionStatusFlag.SKIPPED -> DISCORD_RED
		QuestionStatusFlag.FLAGGED -> DISCORD_YELLOW
		QuestionStatusFlag.ANSWERED -> DISCORD_BLURPLE
		QuestionStatusFlag.NO_FLAG -> null
	}

	when (flag) {
		QuestionStatusFlag.FLAGGED ->
			if (flaggedBy != null) {
				footer {
					text = "Flagged by ${flaggedBy.uniqueName}"
					icon = flaggedBy.avatar?.cdnUrl?.toUrl()
				}
			}

		QuestionStatusFlag.CLAIMED ->
			if (claimedOrSkippedBy != null) {
				footer {
					text = "Claimed by ${claimedOrSkippedBy.uniqueName}"
					icon = claimedOrSkippedBy.avatar?.cdnUrl?.toUrl()
				}
			}

		QuestionStatusFlag.SKIPPED ->
			if (claimedOrSkippedBy != null) {
				footer {
					text = "Skipped by ${claimedOrSkippedBy.uniqueName}"
					icon = claimedOrSkippedBy.avatar?.cdnUrl?.toUrl()
				}
			}

		QuestionStatusFlag.ANSWERED ->
			if (claimedOrSkippedBy != null) {
				val answerMethod = if (viaStage == true) "stage" else "text"
				footer {
					text = "Question answered via $answerMethod by ${claimedOrSkippedBy.uniqueName}"
				}
			}

		else -> footer
	}
}

public suspend inline fun ComponentContainer.questionComponents(
	embedMessage: Message,
	interactionUser: User,
	pkMember: PKMember?,
	question: String?,
	answerQueueChannel: GuildMessageChannel?,
	liveChatChannel: GuildMessageChannel?,
	flaggedQueueChannel: GuildMessageChannel?,
	flaggedQuestionChannel: Snowflake?
) {
	ephemeralButton {
		label = "Accept"
		style = ButtonStyle.Success

		action {
			var answerQueueMessage: Message? = null
			answerQueueMessage = answerQueueChannel?.createMessage {
				embed {
					questionEmbed(interactionUser, question, QuestionStatusFlag.ACCEPTED, pkMember)
				}
				components {
					ephemeralButton {
						label = "Claim"
						style = ButtonStyle.Primary

						action {
							val claimer = event.interaction.user
							answerQueueMessage?.edit {
								embed {
									questionEmbed(
										interactionUser,
										question,
										QuestionStatusFlag.CLAIMED,
										pkMember,
										claimedOrSkippedBy = event.interaction.user
									)
								}
								components { removeAll() }
								components {
									answeringButtons(
										claimer,
										liveChatChannel,
										interactionUser,
										pkMember,
										question,
										answerQueueMessage
									)
								}
							}
						}
					}

					ephemeralButton {
						label = "Skip"
						style = ButtonStyle.Secondary

						action {
							answerQueueMessage?.edit {
								embed {
									questionEmbed(
										interactionUser,
										question,
										QuestionStatusFlag.SKIPPED,
										pkMember,
										claimedOrSkippedBy = event.interaction.user
									)
								}
								components { removeAll() }
							}
						}
					}
				}
			}
			embedMessage.edit {
				embed {
					questionEmbed(
						interactionUser,
						question,
						QuestionStatusFlag.ACCEPTED,
						pkMember
					)
				}
				components { removeAll() }
			}
		}
	}

	ephemeralButton {
		label = "Deny"
		style = ButtonStyle.Danger

		action {
			embedMessage.edit {
				embed {
					questionEmbed(interactionUser, question, QuestionStatusFlag.DENIED, pkMember)
				}
				components { removeAll() }
			}
		}
	}

	if (flaggedQuestionChannel != null) {
		ephemeralButton {
			label = "Flag"
			style = ButtonStyle.Secondary

			action {
				var flaggedMessage: Message? = null
				flaggedMessage = flaggedQueueChannel?.createMessage {
					embed {
						questionEmbed(
							interactionUser,
							question,
							QuestionStatusFlag.FLAGGED,
							pkMember,
							flaggedBy = event.interaction.user
						)
					}
					components {
						ephemeralButton {
							label = "Accept"
							style = ButtonStyle.Success

							action {
								var answerQueueMessage: Message? = null
								answerQueueMessage = answerQueueChannel?.createMessage {
									embed {
										questionEmbed(interactionUser, question, QuestionStatusFlag.ACCEPTED, pkMember)
									}
									components {
										ephemeralButton {
											label = "Claim"
											style = ButtonStyle.Primary

											action {
												val claimer = event.interaction.user
												answerQueueMessage?.edit {
													embed {
														questionEmbed(
															interactionUser,
															question,
															QuestionStatusFlag.CLAIMED,
															pkMember,
															claimedOrSkippedBy = event.interaction.user
														)
													}
													components { removeAll() }
													components {
														answeringButtons(
															claimer,
															liveChatChannel,
															interactionUser,
															pkMember,
															question,
															answerQueueMessage
														)
													}
												}
											}
										}

										ephemeralButton {
											label = "Skip"
											style = ButtonStyle.Secondary

											action {
												answerQueueMessage?.edit {
													embed {
														questionEmbed(
															interactionUser,
															question,
															QuestionStatusFlag.SKIPPED,
															pkMember,
															claimedOrSkippedBy = event.interaction.user
														)
													}
													components { removeAll() }
												}
											}
										}
									}
								}
								flaggedMessage?.edit {
									embed {
										questionEmbed(
											interactionUser,
											question,
											QuestionStatusFlag.ACCEPTED,
											pkMember
										)
									}
									components { removeAll() }
								}
								embedMessage.edit {
									embed {
										questionEmbed(
											interactionUser,
											question,
											QuestionStatusFlag.ACCEPTED,
											pkMember
										)
									}
									components { removeAll() }
								}
							}
						}

						ephemeralButton {
							label = "Deny"
							style = ButtonStyle.Danger

							action {
								flaggedMessage?.edit {
									embed {
										questionEmbed(interactionUser, question, QuestionStatusFlag.DENIED, pkMember)
									}
									components { removeAll() }
								}
								embedMessage.edit {
									embed {
										questionEmbed(
											interactionUser,
											question,
											QuestionStatusFlag.DENIED,
											pkMember
										)
									}
									components { removeAll() }
								}
							}
						}
					}
				}
				embedMessage.edit {
					embed {
						questionEmbed(
							interactionUser,
							question,
							QuestionStatusFlag.FLAGGED,
							pkMember,
							flaggedBy = event.interaction.user
						)
					}
					components { removeAll() }
				}
			}
		}
	}
}

public suspend inline fun ComponentContainer.answeringButtons(
	claimer: User,
	liveChatChannel: GuildMessageChannel?,
	interactionUser: User,
	pkMember: PKMember?,
	question: String?,
	answerQueueMessage: Message?
) {
	ephemeralButton {
		label = "Stage"
		style = ButtonStyle.Success

		check {
			if (event.interaction.user != claimer) {
				fail("You did not claim this question! ${claimer.tag} did")
			}
		}

		action {
			liveChatChannel?.createEmbed {
				questionEmbed(
					interactionUser,
					question,
					QuestionStatusFlag.ANSWERED,
					pkMember,
					claimedOrSkippedBy = claimer,
					viaStage = true
				)
			}
			answerQueueMessage!!.edit { components { removeAll() } }
		}
	}

	ephemeralButton {
		label = "Text"
		style = ButtonStyle.Success
		check {
			if (event.interaction.user != claimer) {
				fail("You did not claim the question! ${claimer.tag} did")
			}
		}

		action {
			liveChatChannel?.createEmbed {
				questionEmbed(
					interactionUser,
					question,
					QuestionStatusFlag.ANSWERED,
					pkMember,
					claimedOrSkippedBy = claimer,
					viaStage = false
				)
			}
			answerQueueMessage!!.edit { components { removeAll() } }
		}
	}
}

@Suppress("MagicNumber")
internal object ColorHexCodeSerializer : KSerializer<Color> {
	override val descriptor: SerialDescriptor =
		PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): Color =
		Color(decoder.decodeString().trimStart('#').toInt(16))

	override fun serialize(encoder: Encoder, value: Color) {
		encoder.encodeString(value.rgb.toString(16))
	}
}
