@file:OptIn(ExperimentalTime::class)

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.entity.Permission
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

private val TASK_DELAY = Duration.hours(1)
private val MAX_PENDING_DURATION = Duration.days(MAX_PENDING_DAYS)

class UserCleanupExtension : Extension() {
    override val name: String = "user-cleanup"

    private val servers: ServerSettingsCollection by inject()

    private val scheduler = Scheduler()
    private lateinit var task: Task

    override suspend fun setup() {
        task = scheduler.schedule(TASK_DELAY, pollingSeconds = 60, callback = ::taskRun)

        GUILDS.forEach { guildId ->
            ephemeralSlashCommand {
                name = "cleanup-users"
                description = "Clean up likely bot accounts"

                guild(guildId)

                check { hasPermissionInMainGuild(Permission.Administrator) }

                action {
                    val removed = taskRun()

                    respond {
                        content = "Kicked $removed accounts across all Quilt servers."
                    }
                }
            }
        }
    }

    suspend fun taskRun(): Int {
        var removed = 0
        val cutoff = Clock.System.now() - MAX_PENDING_DURATION

        try {
            val guilds = servers
                .getByQuiltServers()
                .toList()
                .mapNotNull { kord.getGuild(it._id) }

            guilds.forEach { guild ->
                guild.members
                    .filter { it.isPending && it.joinedAt <= cutoff }
                    .toList()
                    .forEach {
                        it.kick("Failed to pass member screening within $MAX_PENDING_DAYS days.")
                        removed += 1
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
}
