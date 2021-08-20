package org.quiltmc.community

import com.kotlindiscord.kord.extensions.checks.*
import com.kotlindiscord.kord.extensions.checks.types.Check
import mu.KotlinLogging

val inQuiltGuild: Check<*> = {
    val logger = KotlinLogging.logger("org.quiltmc.community.inQuiltGuild")
    val guild = guildFor(event)

    if (guild == null) {
        logger.nullGuild(event)

        fail("Must be in one of the Quilt servers")
    } else {
        if (guild.id !in GUILDS) {
            fail("Must be in one of the Quilt servers")
        }
    }
}

val hasBaseModeratorRole: Check<*> = {
    inQuiltGuild(this)

    if (this.passed) {  // They're on a Quilt guild
        val logger = KotlinLogging.logger("org.quiltmc.community.hasBaseModeratorRole")
        val member = memberFor(event)?.asMemberOrNull()

        if (member == null) {  // Shouldn't happen, but you never know
            logger.nullMember(event)

            fail()
        } else {
            if (!member.roleIds.any { it in MODERATOR_ROLES }) {
                logger.failed("Member does not have a Quilt base moderator role")

                fail("Must be a Quilt moderator, with the `Moderators` role")
            }
        }
    }
}
