package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.deleteIgnoringNotFound
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkType
import org.quiltmc.community.GUILDS
import org.quiltmc.community.api.PhishingApi
import org.quiltmc.community.hasBaseModeratorRole
import org.quiltmc.community.inQuiltGuild
import org.quiltmc.community.notHasBaseModeratorRole

class PhishingExtension : Extension() {
    override val name: String = "phishing"

    private val logger = KotlinLogging.logger { }

    val extractor: LinkExtractor = LinkExtractor.builder()
        .linkTypes(setOf(LinkType.URL, LinkType.WWW))
        .build()

    val api = PhishingApi()

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check { inQuiltGuild() }
            check { notHasBaseModeratorRole() }
            check { isNotBot() }
            check { event.message.author != null }

            action {
                if (hasBadDomain(event.message)) {
                    event.message.deleteIgnoringNotFound()

                    event.launch {
                        event.message.author!!.dm {
                            content = "The following message was removed, as it contained at least one scam domain."

                            embed {
                                color = DISCORD_RED
                                description = event.message.content

                                field {
                                    name = "Channel"
                                    value = event.message.channel.mention
                                }

                                field {
                                    name = "Server"
                                    value = event.message.getGuild().name
                                }
                            }
                        }
                    }

                    event.getGuild()?.getCozyLogChannel()?.createEmbed {
                        title = "Scam domain detected"
                        color = DISCORD_RED
                        description = event.message.content

                        field {
                            name = "Author"
                            value = "${event.message.author!!.mention} (" +
                                    "`${event.message.author!!.id.asString}` / " +
                                    "`${event.message.author!!.tag}`" +
                                    ")"
                        }

                        field {
                            name = "Channel"
                            value = "${event.message.channel.mention} (" +
                                    "`${event.message.channel.id.asString}` / " +
                                    ")"
                        }

                        field {
                            name = "Message"
                            value = "[`${event.message.id.asString}`](${event.message.getJumpUrl()})"
                        }
                    }
                }
            }
        }

        GUILDS.forEach { quiltGuildId ->
            ephemeralSlashCommand(::DomainArgs) {
                name = "phishing-check"
                description = "Check whether a given domain is a known phishing domain."

                check { hasBaseModeratorRole() }
                guild(quiltGuildId)

                action {
                    respond {
                        content = if (api.checkDomain(arguments.domain)) {
                            "✅ `${arguments.domain}` is a known phishing domain."
                        } else {
                            "❌ `${arguments.domain}` is not a known phishing domain."
                        }
                    }
                }
            }

            ephemeralMessageCommand {
                name = "Phishing Check"

                check { hasBaseModeratorRole() }
                guild(quiltGuildId)

                action {
                    respond {
                        content = if (targetMessages.any { hasBadDomain(it) }) {
                            "✅ this message contains a known phishing domain."
                        } else {
                            "❌ this message does not contain a known phishing domain."
                        }
                    }
                }
            }
        }
    }

    suspend inline fun hasBadDomain(content: String): Boolean =
        extractUrlInfo(content).any { (_, domain) ->
            api.checkDomain(domain)
        }

    suspend inline fun hasBadDomain(message: Message): Boolean = hasBadDomain(message.content)

    fun extractUrlInfo(content: String): Set<Pair<String?, String>> {  // Pair(scheme, domain)
        val links = extractor.extractLinks(content)
        val foundPairs: MutableSet<Pair<String?, String>> = mutableSetOf()

        for (link in links) {
            var domain = content.substring(link.beginIndex, link.endIndex)
            var scheme: String? = null

            if ("://" in domain) {
                val split = domain.split("://")

                scheme = split[0]
                domain = split[1]
            }

            if ("/" in domain) {
                domain = domain.split("/").first()
            }

            logger.debug { "Found link ($link) - $scheme / $domain" }

            foundPairs += Pair(scheme, domain)
        }

        logger.debug { "Found ${foundPairs.size} links." }

        return foundPairs
    }

    suspend fun Guild.getCozyLogChannel() =
        channels.firstOrNull { it.name == "cozy-logs" }
            ?.asChannelOrNull() as? GuildMessageChannel

    inner class DomainArgs : Arguments() {
        val domain by string("domain", "Domain name to check") { _, value ->
            if ("/" in value) {
                throw DiscordRelayedException("Please provide the domain name only, without the protocol or a path.")
            }
        }
    }
}
