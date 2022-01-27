/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.cleanup

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import mu.KotlinLogging
import org.quiltmc.community.cozy.modules.cleanup.config.UserCleanupConfig
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MEMBER_CHUNK_SIZE = 15

/**
 * User cleanup extension, handles the cleanup of pending users after they've lurked for a while.
 */
public class UserCleanupExtension(
    private val config: UserCleanupConfig
) : Extension() {
    override val name: String = UserCleanupPlugin.id
    private lateinit var instantFormatter: DateTimeFormatter

    private val logger = KotlinLogging.logger {}
    private val scheduler = Scheduler()

    private lateinit var task: Task

    override suspend fun setup() {
        instantFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            .withLocale(config.dateFormattingLocale)
            .withZone(ZoneId.systemDefault())

        if (config.runAutomatically) {
            task = scheduler.schedule(config.getTaskDelay(), pollingSeconds = 60, callback = ::taskRun)
        }

        if (!config.registerCommand) {
            return
        }

        ephemeralSlashCommand(::CleanupArgs) {
            name = "cleanup-users"
            description = "Clean up user accounts that haven't passed member screening"

            check { anyGuild() }

            config.getCommandChecks().forEach(::check)

            check {
                val guild = guildFor(event)?.asGuild()

                if (guild != null) {
                    failIf("This server isn't configured for user cleanup.") { !config.checkGuild(guild) }
                }
            }

            action {
                val removed = processGuild(guild!!.asGuild(), arguments.dryRun)

                if (removed.isEmpty()) {
                    respond {
                        content = "It doesn't look like there's anyone that needs to be removed."
                    }

                    return@action
                }

                editingPaginator {
                    removed.chunked(MEMBER_CHUNK_SIZE).forEach { chunk ->
                        page {
                            color = DISCORD_FUCHSIA
                            title = "User Cleanup"

                            if (arguments.dryRun) {
                                color = DISCORD_BLURPLE
                                title += " (dry-run)"
                            }

                            description = "**Mention** | **Tag** | **Join date**\n\n"

                            chunk.forEach { member ->
                                description += "${member.mention} | ${member.tag} |" +
                                        "${member.joinedAt.toDiscord(TimestampType.Default)}\n"
                            }
                        }
                    }
                }.send()
            }
        }
    }

    private suspend fun processGuild(guild: Guild, dryRun: Boolean): MutableSet<Member> {
        val pendingDuration = config.getMaxPendingDuration()
        val removed: MutableSet<Member> = mutableSetOf()
        val now = Clock.System.now()

        guild.members
            .filter { it.isPending && !it.isBot }
            .filter { (it.joinedAt + pendingDuration) <= now }
            .toList()
            .forEach {
                if (!dryRun) {
                    it.kick("Didn't pass member screening quickly enough.")
                }

                removed += it
            }

        return removed
    }

    private suspend fun taskRun() {
        val guilds = kord.guilds
            .filter { config.checkGuild(it) }
            .toList()

        try {
            guilds.forEach { guild ->
                val removed = processGuild(guild, false)

                if (removed.isNotEmpty()) {
                    logger.info { "Removed ${removed.size} users from ${guild.name} (${guild.id})" }

                    val table = "| User ID | Tag | Join Date (UTC) |\n" +
                            "| ------- | --- | --------------- |\n" +

                            removed.joinToString("\n") {
                                "| ${it.id} | ${it.tag} | ${instantFormatter.format(it.joinedAt.toJavaInstant())} |"
                            }

                    config.getLoggingChannel(guild).createMessage {
                        content = "**User Cleanup:** Cleaned up ${removed.size} users that didn't pass member " +
                                "screening quickly enough."

                        addFile("users.md", table.byteInputStream())
                    }
                }
            }
        } finally {
            if (::task.isInitialized) {
                task.restart()
            }
        }
    }

    override suspend fun unload() {
        super.unload()

        if (::task.isInitialized) {
            task.cancel()
        }
    }

    internal inner class CleanupArgs : Arguments() {
        internal val dryRun by defaultingBoolean {
            name = "dry-run"
            description = "Whether to preview the members to kick instead of actually kicking them"

            defaultValue = true
        }
    }
}
