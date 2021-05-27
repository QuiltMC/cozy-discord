package org.quiltmc.community.extensions

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.checks.or
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.deltas.MemberDelta
import com.kotlindiscord.kord.extensions.utils.hasPermission
import com.kotlindiscord.kord.extensions.utils.hasRole
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.withTyping
import dev.kord.core.entity.Guild
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.koin.core.component.inject
import org.quiltmc.community.GUILDS
import org.quiltmc.community.inQuiltGuild

private val BAN_PERMS: Array<Permission> = arrayOf(Permission.BanMembers, Permission.Administrator)
private val ROLE_PERMS: Array<Permission> = arrayOf(Permission.ManageRoles, Permission.Administrator)

private const val TUPPER_ROLE_NAME: String = "Tupperbox Access"

class SyncExtension : Extension() {
    override val name: String = "sync"

    private val logger = KotlinLogging.logger {}
    private val kord: Kord by inject()

    @Suppress("SpreadOperator")  // No better way atm, and performance impact is negligible
    override suspend fun setup() {
        val banPermsCheck = or(*BAN_PERMS.map { hasPermission(it) }.toTypedArray())
        val rolePermsCheck = or(*ROLE_PERMS.map { hasPermission(it) }.toTypedArray())

        group {
            name = "sync"
            description = "Synchronisation commands."

            command {
                name = "bans"
                description = "Additively synchronise bans between all servers, so that everything matches."

                check(inQuiltGuild, banPermsCheck)
                requirePermissions(Permission.BanMembers)

                action {
                    val guilds = getGuilds()

                    logger.info { "Syncing bans for ${guilds.size} guilds." }

                    guilds.forEach {
                        logger.debug { "${it.id.value} -> ${it.name}" }

                        val member = it.getMember(this@SyncExtension.kord.selfId)

                        if (!BAN_PERMS.any { perm -> member.hasPermission(perm) }) {
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

            command {
                name = "roles"
                description = "Additively synchronise roles between all servers, so that everything matches."

                check(inQuiltGuild, rolePermsCheck)
                requirePermissions(Permission.ManageRoles)

                action {
                    val guilds = getGuilds()

                    logger.info { "Syncing roles for ${guilds.size} guilds." }

                    guilds.forEach {
                        logger.debug { "${it.id.value} -> ${it.name}" }

                        val member = it.getMember(this@SyncExtension.kord.selfId)

                        if (!ROLE_PERMS.any { perm -> member.hasPermission(perm) }) {
                            message.respond(
                                "I don't have permission to manage roles on ${it.name} (`${it.id.value}`)"
                            )

                            return@action
                        }
                    }

                    val membersWithRole: MutableSet<Snowflake> = mutableSetOf()
                    var newAssignments = 0L

                    message.channel.withTyping {
                        guilds.forEach { guild ->
                            guild.members.collect { member ->
                                if (
                                    member.roles.toList().firstOrNull {
                                        it.name.equals(TUPPER_ROLE_NAME, true)
                                    } != null
                                ) {
                                    membersWithRole.add(member.id)
                                }
                            }
                        }

                        for (guild in guilds) {
                            val role = guild.roles.toList().firstOrNull {
                                it.name.equals(TUPPER_ROLE_NAME, true)
                            }

                            if (role == null) {
                                message.respond(pingInReply = false) {
                                    content = "**Note:** Guild `${guild.name}` seems to be missing a " +
                                            "`$TUPPER_ROLE_NAME` role."
                                }

                                continue
                            }

                            for (id in membersWithRole) {
                                val member = guild.getMemberOrNull(id) ?: continue

                                if (!member.hasRole(role)) {
                                    member.addRole(role.id, "Synchronised from another server.")
                                    newAssignments += 1
                                }
                            }
                        }

                        message.respond {
                            embed {
                                title = "Roles synced"

                                description = "Added $newAssignments missing roles."
                            }
                        }
                    }
                }
            }
        }

        event<BanAddEvent> {
            check(inQuiltGuild)

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
            check(inQuiltGuild)

            action {
                val guilds = getGuilds().filter { it.id != event.guildId }

                guilds.forEach {
                    if (it.getBanOrNull(event.user.id) != null) {
                        it.unban(event.user.id)
                    }
                }
            }
        }

        event<MemberUpdateEvent> {
            check(inQuiltGuild)

            action {
                val delta = MemberDelta.from(event.old, event.member) ?: return@action
                val roles = delta.roles.value

                val addedRole = roles?.added?.first { it.name.equals(TUPPER_ROLE_NAME, true) }
                val removedRole = roles?.removed?.first { it.name.equals(TUPPER_ROLE_NAME, true) }

                val guilds = getGuilds().filter { it.id != event.guildId }

                if (addedRole != null) {
                    for (guild in guilds) {
                        val member = guild.getMemberOrNull(event.member.id) ?: continue

                        val role = guild.roles.toList().firstOrNull {
                            it.name.equals(TUPPER_ROLE_NAME, true)
                        } ?: continue

                        if (!member.hasRole(role)) {
                            member.addRole(role.id, "Synchronised from ${event.guild.asGuild().name}.")
                        }
                    }
                } else if (removedRole != null) {
                    for (guild in guilds) {
                        val member = guild.getMemberOrNull(event.member.id) ?: continue

                        val role = guild.roles.toList().firstOrNull {
                            it.name.equals(TUPPER_ROLE_NAME, true)
                        } ?: continue

                        if (member.hasRole(role)) {
                            member.removeRole(role.id, "Synchronised from ${event.guild.asGuild().name}.")
                        }
                    }
                }
            }
        }
    }

    private suspend fun getGuilds() = GUILDS.mapNotNull { kord.getGuild(it) }
}
