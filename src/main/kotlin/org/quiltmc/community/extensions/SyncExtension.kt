package org.quiltmc.community.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.hasPermission
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.entity.Guild
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import kotlinx.coroutines.flow.toList
import org.quiltmc.community.GUILDS

class SyncExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name: String = "sync"

    override suspend fun setup() {
        command {
            name = "sync"
            description = "Additively synchronise bans between all servers, so that everything matches."

            check(hasPermission(Permission.BanMembers))
            requirePermissions(Permission.BanMembers)

            action {
                val guilds = getGuilds()

                guilds.forEach {
                    if (!it.getMember(bot.kord.selfId).hasPermission(Permission.BanMembers)) {
                        message.respond(
                            "I don't have permission to ban members on ${it.name} (`${it.id.value}`)"
                        )

                        return@action
                    }
                }

                val allBans: MutableMap<Snowflake, String?> = mutableMapOf()

                guilds.forEach { guild ->
                    guild.bans.toList().forEach { ban ->
                        if (allBans[ban.userId] == null || ban.reason?.startsWith("Synced:") == false) {
                            // If it's null/not present or the given ban entry doesn't start with "Synced:"
                            allBans[ban.userId] = ban.reason
                        }
                    }
                }

                val syncedBans: MutableMap<Guild, Int> = mutableMapOf()

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

    suspend fun getGuilds() = GUILDS.mapNotNull { bot.kord.getGuild(Snowflake(it)) }
}
