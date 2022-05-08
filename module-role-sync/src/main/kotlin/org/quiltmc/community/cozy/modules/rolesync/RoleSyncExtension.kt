/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.rolesync

import com.kotlindiscord.kord.extensions.annotations.DoNotChain
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.hasRole
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import kotlinx.coroutines.flow.filter
import org.quiltmc.community.cozy.modules.rolesync.config.RoleSyncConfig

/**
 * Module providing role sync functionality.
 *
 * When one of the source roles is added or removed from a user, the user is added or removed from the target role.
 */
public class RoleSyncExtension(
    private val config: RoleSyncConfig
) : Extension() {
    override val name: String = RoleSyncPlugin.id

    @OptIn(DoNotChain::class)
    override suspend fun setup() {
        event<MemberUpdateEvent> {
            action {
                // Make sure the old member is available
                event.old ?: return@action

                // Check if the roles have changed
                if (event.old!!.roleIds == event.member.roleIds) { return@action }

                for (role in config.getRolesToSync()) {
                    if (!event.old!!.roleIds.contains(role.source) && event.member.roleIds.contains(role.source)) {
                        // Role was added

                        getGuildForRoleSnowflake(role.target, bot).guild
                            .getMemberOrNull(event.member.id)
                            ?.addRole(role.target, "Role synced")
                    } else if (
                        event.old!!.roleIds.contains(role.source) && !event.member.roleIds.contains(role.source)
                    ) {
                        // Role was removed

                        getGuildForRoleSnowflake(role.target, bot).guild
                            .getMemberOrNull(event.member.id)
                            ?.removeRole(role.target, "Role synced")
                    }
                }
            }
        }

        event<MemberJoinEvent> {
            action {
                for (role in config.getRolesToSync()) {
                    if (
                        getGuildForRoleSnowflake(role.source, bot).guild
                            .getMemberOrNull(event.member.id)
                            ?.roleIds
                            ?.contains(role.source) == true
                    ) {
                        event.member.addRole(role.target, "Role synced")
                    }
                }
            }
        }

        ephemeralSlashCommand() {
            name = "role-sync"
            description = "Manually sync roles"

            config.getCommandChecks().forEach(::check)

            action {
                var added = 0
                var removed = 0

                for (role in config.getRolesToSync()) {
                    val targetRole = getGuildForRoleSnowflake(role.target, bot)
                    val sourceRole = getGuildForRoleSnowflake(role.source, bot)

                    // Check if the target role should be removed
                    targetRole.guild.members
                        .filter { it.roleIds.contains(role.target) }  // Has the target role
                        .filter {
                            sourceRole.guild.getMemberOrNull(it.id)?.hasRole(sourceRole) == false
                        }  // Doesn't have the source role
                        .collect {
                            it.removeRole(role.target, "Role synced")
                            removed++
                        }

                    // Check if the target role should be added
                    sourceRole.guild.members
                        .filter { it.roleIds.contains(role.source) }  // Has the source role
                        .filter {
                            targetRole.guild.getMemberOrNull(it.id)?.hasRole(targetRole) == false
                        }  // Doesn't have the target role
                        .collect {
                            targetRole.guild.getMemberOrNull(it.id)?.addRole(role.target, "Role synced")
                            added++
                        }
                }

                respond { content = "$added role(s) added, $removed role(s) removed" }
            }
        }
    }
}
