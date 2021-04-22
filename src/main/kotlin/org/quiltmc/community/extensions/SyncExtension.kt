package org.quiltmc.community.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.checks.inGuild
import com.kotlindiscord.kord.extensions.checks.or
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.hasPermission
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.entity.Guild
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.quiltmc.community.GUILDS

private val SYNC_PERMS: Array<Permission> = arrayOf(Permission.BanMembers, Permission.Administrator)

class SyncExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "sync"

    private val logger = KotlinLogging.logger {}

    @Suppress("SpreadOperator")  // No better way atm, and performance impact is negligible
    override suspend fun setup() {
        val guildCheck = or(*GUILDS.map { inGuild(Snowflake(it)) }.toTypedArray())
        val permsCheck = or(*SYNC_PERMS.map { hasPermission(it) }.toTypedArray())

        command {
            name = "sync"
            description = "Additively synchronise bans between all servers, so that everything matches."

            check(guildCheck, permsCheck)
            requirePermissions(Permission.BanMembers)

            action {
                val guilds = getGuilds()

                logger.info { "Syncing bans for ${guilds.size} guilds." }

                guilds.forEach {
                    logger.info { "${it.id.value} -> ${it.name}" }

                    val member = it.getMember(bot.kord.selfId)

                    if (!SYNC_PERMS.any { perm -> member.hasPermission(perm) }) {
                        message.respond(
                            "I don't have permission to ban members on ${it.name} (`${it.id.value}`)"
                        )

                        return@action
                    }
                }

                val allBans: MutableMap<Snowflake, String?> = mutableMapOf()
                val syncedBans: MutableMap<Guild, Int> = mutableMapOf()

                message.channel.withTyping {
                    guilds.forEach { guild ->
                        guild.bans.toList().forEach { ban ->
                            if (allBans[ban.userId] == null || ban.reason?.startsWith("Synced:") == false) {
                                // If it's null/not present or the given ban entry doesn't start with "Synced:"
                                allBans[ban.userId] = ban.reason
                            }
                        }
                    }

                    guilds.forEach { guild ->
                        allBans.forEach { (userId, reason) ->
                            if (guild.getBanOrNull(userId) == null) {
                                syncedBans[guild] = (syncedBans[guild] ?: 0) + 1

                                guild.ban(userId) {
                                    this.reason = "Synced: " + (reason ?: "No reason given")
                                }
                            }
                        }
                    }

                    message.respond {
                        embed {
                            title = "Bans synced"

                            description = syncedBans.map { "**${it.key.name}**: ${it.value} added" }
                                .joinToString("\n")
                        }
                    }
                }
            }
        }

        event<BanAddEvent> {
            action {
                val guilds = getGuilds().filter { it.id != event.guildId }
                val ban = event.getBan()

                guilds.forEach {
                    if (it.getBanOrNull(ban.userId) == null) {
                        it.ban(ban.userId) {
                            this.reason = "Synced: " + (ban.reason ?: "No reason given")
                        }
                    }
                }
            }
        }

        event<BanRemoveEvent> {
            action {
                val guilds = getGuilds().filter { it.id != event.guildId }

                guilds.forEach {
                    if (it.getBanOrNull(event.user.id) != null) {
                        it.unban(event.user.id)
                    }
                }
            }
        }
    }

    private suspend fun getGuilds() = GUILDS.mapNotNull { bot.kord.getGuild(Snowflake(it)) }
}
