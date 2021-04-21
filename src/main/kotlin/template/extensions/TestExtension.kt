package template.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.defaultingCoalescedString
import com.kotlindiscord.kord.extensions.commands.converters.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.user
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.respond
import com.kotlindiscord.kord.extensions.utils.startsWithVowel
import dev.kord.common.annotation.KordPreview
import template.TEST_SERVER_ID

@OptIn(KordPreview::class)
class TestExtension(bot: ExtensibleBot) : Extension(bot) {
    override val name = "test"

    override suspend fun setup() {
        command(::SlapArgs) {
            name = "slap"
            description = "Ask the bot to slap another user"

            check { event -> event.message.author != null }

            action {
                with(arguments) {
                    // Don't slap ourselves on request, slap the requester!
                    val realTarget = if (target.id == bot.kord.selfId) {
                        message.author!!
                    } else {
                        target
                    }

                    // Use "a" or "an" as appropriate for English
                    val indefiniteArticle = if (weapon!!.startsWithVowel()) {
                        "an"
                    } else {
                        "a"
                    }

                    message.respond("*slaps ${realTarget.mention} with $indefiniteArticle $weapon*")
                }
            }
        }

        slashCommand(::SlapSlashArgs) {
            name = "slap"
            description = "Ask the bot to slap another user"
            showSource = true

            guild(TEST_SERVER_ID)  // Otherwise it'll take an hour to update

            action {
                with(arguments) {
                    // Don't slap ourselves on request, slap the requester!
                    val realTarget = if (target.id == bot.kord.selfId) {
                        member
                    } else {
                        target
                    }

                    // Use "a" or "an" as appropriate for English
                    val indefiniteArticle = if (weapon.startsWithVowel()) {
                        "an"
                    } else {
                        "a"
                    }

                    followUp("*slaps ${realTarget.mention} with $indefiniteArticle $weapon*")
                }
            }
        }
    }

    class SlapArgs : Arguments() {
        val target by user("target", description = "Person you want to slap")

        // This is nullable due to a typo - it won't be in future releases!
        val weapon by defaultingCoalescedString(
            "weapon",

            defaultValue = "large, smelly trout",
            description = "What you want to slap with"
        )
    }

    class SlapSlashArgs : Arguments() {
        val target by user("target", description = "Person you want to slap")

        // Coalesced strings are not currently supported by slash commands
        val weapon by defaultingString(
            "weapon",

            defaultValue = "large, smelly trout",
            description = "What you want to slap with"
        )
    }
}
