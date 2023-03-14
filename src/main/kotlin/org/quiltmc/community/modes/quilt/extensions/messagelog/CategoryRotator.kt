/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions.messagelog

import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.edit
import dev.kord.core.behavior.createTextChannel
import dev.kord.core.entity.channel.Category
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.quiltmc.community.COLOUR_NEGATIVE
import org.quiltmc.community.COLOUR_POSITIVE
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoField
import java.util.*
import kotlin.math.abs

private const val WEEK_DIFFERENCE = 5L

private const val CHECK_DELAY = 1000L * 60L * 30L  // 30 minutes

private val NAME_REGEX = Regex("message-log-(\\d{4})-(\\d{2})")

class CategoryRotator(private val category: Category, private val modLog: GuildMessageChannel) {
	private val guild get() = category.guild
	private val channel get() = channels.last()

	private var channels: List<GuildMessageChannel> = listOf()
	private var checkJob: Job? = null

	private val logger = KotlinLogging.logger { }
	private val rotationLock = Mutex()

	fun start() {
		checkJob = category.kord.launch {
			populate()
			loop()
		}
	}

	fun stop() {
		checkJob?.cancel()
		checkJob = null
	}

	suspend fun loop() {
		while (true) {
			delay(CHECK_DELAY)

			logger.debug { "Running scheduled channel population." }
			populate()
		}
	}

	suspend fun send(messageBuilder: suspend UserMessageCreateBuilder.() -> Unit) =
		rotationLock.withLock {
			channel.createMessage {
				messageBuilder()
			}
		}

	suspend fun populate() {
		rotationLock.withLock {
			@Suppress("TooGenericExceptionCaught")  // Anything could happen, really
			try {
				val now = OffsetDateTime.now(ZoneOffset.UTC)
				val thisWeek = now.getLong(ChronoField.ALIGNED_WEEK_OF_YEAR)
				val thisYear = now.getLong(ChronoField.YEAR)

				var currentChannelExists = false
				val allChannels = mutableListOf<TopGuildMessageChannel>()

				category.channels.toList().forEach {
					if (it is TopGuildMessageChannel) {
						logger.debug { "Checking existing channel: ${it.name}" }

						val match = NAME_REGEX.matchEntire(it.name)

						if (match != null) {
							val year = match.groups[1]!!.value.toLong()
							val week = match.groups[2]!!.value.toLong()
							val yearWeeks = getTotalWeeks(year.toInt())

							val weekDifference = abs(thisWeek - week)
							val yearDifference = abs(thisYear - year)

							if (year == thisYear && week == thisWeek) {
								logger.debug { "Passing: This is the latest channel." }

								currentChannelExists = true
								allChannels.add(it)
							} else if (year > thisYear) {
								// It's in the future, so this isn't valid!
								logger.debug { "Deleting: This is next year's channel." }

								it.delete()
								logDeletion(it)
							} else if (year == thisYear && week > thisWeek) {
								// It's in the future, so this isn't valid!
								logger.debug { "Deleting: This is a future week's channel." }

								it.delete()
								logDeletion(it)
							} else if (
								yearDifference > 1L || yearDifference != 1L && weekDifference > WEEK_DIFFERENCE
							) {
								// This one is _definitely_ too old.
								logger.debug { "Deleting: This is an old channel." }

								it.delete()
								logDeletion(it)
							} else if (yearDifference == 1L && yearWeeks - week + thisWeek > WEEK_DIFFERENCE) {
								// This is from last year, but more than 5 weeks ago.
								logger.debug { "Deleting: This is an old channel from last year." }

								it.delete()
								logDeletion(it)
							} else {
								allChannels.add(it)
							}
						}
					}
				}

				@Suppress("MagicNumber")
				if (!currentChannelExists) {
					logger.debug { "Creating this week's channel." }

					val yearPadded = thisYear.toString().padStart(4, '0')
					val weekPadded = thisWeek.toString().padStart(2, '0')

					val c = guild.asGuild().createTextChannel("message-log-$yearPadded-$weekPadded") {
						parentId = category.id
					}

					currentChannelExists = true

					logCreation(c)
					allChannels.add(c)
				}

				allChannels.sortBy { it.name }

				while (allChannels.size > WEEK_DIFFERENCE) {
					val c = allChannels.removeFirst()

					logger.debug { "Deleting extra channel: ${c.name}" }

					c.delete()
					logDeletion(c)
				}

				channels = allChannels

				logger.debug { "Sorting channels." }

				allChannels.forEachIndexed { i, c ->
					val curPos = c.rawPosition

					if (curPos != i) {
						logger.debug { "Updating channel position for ${c.name}: $curPos -> $i" }

						(allChannels[i] as TextChannel).edit {
							position = i
						}
					}
				}

				logger.debug { "Done." }
			} catch (t: Throwable) {
				logger.error(t) { "Error thrown during rotation." }
			}
		}
	}

	@Suppress("MagicNumber")  // It's the days in december, c'mon
	private fun getTotalWeeks(year: Int): Int {
		val cal = Calendar.getInstance()

		cal.set(Calendar.YEAR, year)
		cal.set(Calendar.MONTH, Calendar.DECEMBER)
		cal.set(Calendar.DAY_OF_MONTH, 31)

		return cal.getActualMaximum(Calendar.WEEK_OF_YEAR)
	}

	private suspend fun logCreation(channel: GuildMessageChannel) = modLog.createEmbed {
		title = "Message log rotation"
		color = COLOUR_POSITIVE

		description = "Channel created: **#${channel.name} (`${channel.id}`)**"
	}

	private suspend fun logDeletion(channel: GuildMessageChannel) = modLog.createEmbed {
		title = "Message log rotation"
		color = COLOUR_NEGATIVE

		description = "Channel removed: **#${channel.name} (`${channel.id}`)**"
	}
}
