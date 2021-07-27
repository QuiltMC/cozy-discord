package org.quiltmc.community

import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.nullGuild
import com.kotlindiscord.kord.extensions.checks.types.Check
import mu.KotlinLogging

val inQuiltGuild: Check<*> = {
    val logger = KotlinLogging.logger("com.kotlindiscord.kord.extensions.checks.inGuild")
    val guild = guildFor(event)

    if (guild == null) {
        logger.nullGuild(event)

        fail()
    } else {
        if (guild.id !in GUILDS) {
            fail("Must be in one of the Quilt servers")
        }
    }
}
