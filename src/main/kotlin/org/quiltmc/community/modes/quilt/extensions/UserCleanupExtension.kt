@file:OptIn(ExperimentalTime::class)

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import org.quiltmc.community.GUILDS
import org.quiltmc.community.database.collections.ServerSettingsCollection
import org.quiltmc.community.hasPermissionInMainGuild
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private const val MAX_PENDING_DAYS = 3
private const val MEMBER_CHUNK_SIZE = 10

// private val TASK_DELAY = Duration.hours(1)
private val MAX_PENDING_DURATION = Duration.days(MAX_PENDING_DAYS)

class UserCleanupExtension : Extension() {
    override val name: String = "user-cleanup"

    private val servers: ServerSettingsCollection by inject()

    // private val scheduler = Scheduler()
    private lateinit var task: Task

    override suspend fun setup() {
//        task = scheduler.schedule(TASK_DELAY, pollingSeconds = 60, callback = ::taskRun)

        GUILDS.forEach { guildId ->
            ephemeralSlashCommand(::CleanupArgs) {
                name = "cleanup-users"
                description = "Clean up likely bot accounts"

                guild(guildId)

                check { hasPermissionInMainGuild(Permission.Administrator) }

                action {
                    if (::task.isInitialized) {
                        task.cancel()
                    }

                    val guilds = servers.getByQuiltServers().toList().sortedBy { it.quiltServerType!!.readableName }
                    val removed = taskRun(arguments.dryRun).groupBy { it.guildId }

                    if (removed.isEmpty()) {
                        respond {
                            content = "It doesn't look like there's anyone that needs to be removed."
                        }

                        return@action
                    }

                    editingPaginator {
                        var defaultSet = false

                        for (guild in guilds) {
                            val members = removed[guild._id] ?: continue

                            if (members.isEmpty()) {
                                continue
                            }

                            if (!defaultSet) {
                                defaultSet = true

                                pages.defaultGroup = guild.quiltServerType!!.readableName
                            }

                            members.chunked(MEMBER_CHUNK_SIZE).forEach { chunk ->
                                page(guild.quiltServerType!!.readableName) {
                                    color = DISCORD_FUCHSIA
                                    title = "User Cleanup: ${guild.quiltServerType!!.readableName}"

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
                        }
                    }.send()
                }
            }
        }
    }

    suspend fun taskRun(dryRun: Boolean = false): MutableList<Member> {
        val removed: MutableList<Member> = mutableListOf()
        val now = Clock.System.now()

        try {
            val guilds = servers
                .getByQuiltServers()
                .toList()
                .mapNotNull { kord.getGuild(it._id) }

            guilds.forEach { guild ->
                guild.members
                    .filter { it.isPending && (it.joinedAt + MAX_PENDING_DURATION) <= now }
                    .toList()
                    .forEach {
                        if (!dryRun) {
                            it.kick("Failed to pass member screening within $MAX_PENDING_DAYS days.")
                        }

                        removed += it
                    }
            }

            return removed
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

    inner class CleanupArgs : Arguments() {
        val dryRun by defaultingBoolean(
            "dry-run",
            "Whether to preview the member to kick instead of actually kicking them",
            true
        )
    }
}
