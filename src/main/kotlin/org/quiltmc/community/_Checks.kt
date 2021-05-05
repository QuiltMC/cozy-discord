package org.quiltmc.community

import com.kotlindiscord.kord.extensions.checks.inGuild
import com.kotlindiscord.kord.extensions.checks.or

val inQuiltGuild = or(
    checks = GUILDS.map { inGuild(it) }.toTypedArray()
)
