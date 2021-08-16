package org.quiltmc.community.modes.quilt.extensions.minecraft

import com.kotlindiscord.kord.extensions.DISCORD_FUCHSIA
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.utils.addReaction
import com.kotlindiscord.kord.extensions.utils.respond
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import org.apache.commons.text.StringEscapeUtils
import org.quiltmc.community.inQuiltGuild

private const val PAGINATOR_TIMEOUT = 60_000L  // One minute
private const val CHUNK_SIZE = 10

private const val BASE_URL = "https://launchercontent.mojang.com"
private const val JSON_URL = "$BASE_URL/javaPatchNotes.json"

private const val CHECK_DELAY = 60L
private const val THUMBS_UP = "\uD83D\uDC4D"

private val LINK_REGEX = "<a href=\"(?<url>[^\"]+)\"[^>]*>(?<text>[^<]+)</a>".toRegex()

@Suppress("MagicNumber", "UnderscoresInNumericLiterals")
private val CHANNELS: List<Snowflake> = listOf(
//    Snowflake(828218532671389736L),  // Testing
    Snowflake(838805249271267398L),  // Community
    Snowflake(834195264629243904L),  // Toolchain
)

class MinecraftExtension : Extension() {
    override val name: String = "minecraft"

    private val client = HttpClient {
        install(JsonFeature)
    }

    private val scheduler = Scheduler()

    private var checkTask: Task? = null
    private var knownVersions: MutableSet<String> = mutableSetOf()
    private lateinit var currentEntries: PatchNoteEntries

    @OptIn(KordPreview::class)
    override suspend fun setup() {
        populateVersions()

        checkTask = scheduler.schedule(CHECK_DELAY, callback = ::checkTask)

// Not using this for now as Kord doesn't have what we need to paginate it
//        slashCommand(::CheckArguments) {
//            name = "patch-notes"
//            description = "Get patch notes for the given Minecraft version"
//
//            guild(629369990092554240L)  // Testcord
//
//            action {
//
//                if (!::currentEntries.isInitialized) {
//                    ephemeralFollowUp("Still setting up - try again a bit later!")
//                    return@action
//                }
//
//                val patch = if (arguments.version == null) {
//                    currentEntries.entries.first()
//                } else {
//                    currentEntries.entries.firstOrNull { it.version.equals(arguments.version, true) }
//                }
//
//                if (patch == null) {
//                    ephemeralFollowUp("Unknown version supplied: `${arguments.version}`")
//                    return@action
//                }
//
//                var content = "**${patch.title}**\n\n"
//                val (truncated, remaining) = patch.body.formatHTML().truncateMarkdown(2000)
//
//                content += truncated
//                content += "\n\n[... $remaining more line${if(remaining > 1) "s" else ""}]"
//
//                ephemeralFollowUp(content)
//            }
//        }

        group {
            name = "mc"
            description = "Commands related to Minecraft updates"

            check(inQuiltGuild)
            check(hasPermission(Permission.Administrator))

            command(::CheckArguments) {
                name = "check"
                description = "Retrieve the patch notes for a given Minecraft version, or the latest if not supplied."

                action {
                    if (!::currentEntries.isInitialized) {
                        message.respond("Still setting up - try again a bit later!")
                        return@action
                    }

                    val channelObj = channel.asChannel()

                    val patch = if (arguments.version == null) {
                        currentEntries.entries.first()
                    } else {
                        currentEntries.entries.firstOrNull { it.version.equals(arguments.version, true) }
                    }

                    if (patch == null) {
                        message.respond("Unknown version supplied: `${arguments.version}`")
                        return@action
                    }

                    channelObj.relay(patch)
                }
            }

            command(::CheckArguments) {
                name = "forget"
                description = "Forget a version (the last one by default), allowing it to be relayed again."

                action {
                    if (!::currentEntries.isInitialized) {
                        message.respond("Still setting up - try again a bit later!")
                        return@action
                    }

                    val version = if (arguments.version == null) {
                        currentEntries.entries.first().version
                    } else {
                        currentEntries.entries.firstOrNull {
                            it.version.equals(arguments.version, true)
                        }?.version
                    }

                    if (version == null) {
                        message.respond("Unknown version supplied: `${arguments.version}`")
                        return@action
                    }

                    knownVersions.remove(version)
                    message.addReaction(THUMBS_UP)
                }
            }

            command {
                name = "run"
                description = "Run the check task now, without waiting for it."

                action {
                    message.addReaction(THUMBS_UP)

                    checkTask?.callNow()
                }
            }

            command {
                name = "versions"
                description = "Get a list of patch note versions."

                action {
                    if (!::currentEntries.isInitialized) {
                        message.respond("Still setting up - try again a bit later!")
                        return@action
                    }

                    paginator(targetChannel = channel, targetMessage = message) {
                        owner = message.author
                        timeoutSeconds = PAGINATOR_TIMEOUT

                        currentEntries.entries.chunked(CHUNK_SIZE).forEach { chunk ->
                            page(
                                Page {
                                    title = "Patch note versions"
                                    color = DISCORD_FUCHSIA

                                    description = chunk.joinToString("\n") { "**»** `${it.version}`" }

                                    footer {
                                        text = "${currentEntries.entries.size} versions"
                                    }
                                }
                            )
                        }
                    }.send()
                }
            }
        }
    }

    suspend fun populateVersions() {
        currentEntries = client.get(JSON_URL)

        currentEntries.entries.forEach { knownVersions.add(it.version) }
    }

    suspend fun checkTask() {
        currentEntries = client.get(JSON_URL)

        currentEntries.entries.forEach {
            if (it.version !in knownVersions) {
                relayUpdate(it)
                knownVersions.add(it.version)
            }
        }

        checkTask = scheduler.schedule(CHECK_DELAY, callback = ::checkTask)
    }

    suspend fun relayUpdate(patchNote: PatchNote) =
        CHANNELS.map { kord.getChannelOf<MessageChannel>(it) }
            .filterNotNull()
            .forEach { it.relay(patchNote) }

    fun String.formatHTML(): String {
        var result = this

        result = result.replace("[\n]*</p>\n+<p>[\n]*".toRegex(), "\n\n")
        result = result.replace("[\n]*<[/]*p>[\n]*".toRegex(), "\n")

        result = result.replace("[\n]*<h\\d+>[\n]*".toRegex(), "\n\n**")
        result = result.replace("[\n]*</h\\d+>[\n]*".toRegex(), "**\n")

        result = result.replace("[\n]*<[ou]l>[\n]*".toRegex(), "\n\n")
        result = result.replace("[\n]*</[ou]l>[\n]*".toRegex(), "\n\n")

        result = result.replace("[\n]*</li>\n+<li>[\n]*".toRegex(), "\n**»** ")
        result = result.replace("([\n]{2,})?<li>[\n]*".toRegex(), "\n**»** ")
        result = result.replace("[\n]*</li>[\n]*".toRegex(), "\n\n")

        val links = LINK_REGEX.findAll(result)

        links.forEach {
            result = result.replace(
                it.value,
                "[${it.groups["text"]?.value}](${it.groups["url"]?.value})"
            )
        }

        return StringEscapeUtils.unescapeHtml4(result.trim('\n'))
    }

    fun String.truncateMarkdown(maxLength: Int = 1000): Pair<String, Int> {
        var result = this

        if (length > maxLength) {
            val truncated = result.substring(0, maxLength).substringBeforeLast("\n")
            val remaining = result.substringAfter(truncated).count { it == '\n' }

            result = truncated

            return result to remaining
        }

        return result to 0
    }

    private fun EmbedBuilder.patchNotes(patchNote: PatchNote, maxLength: Int = 1000) {
        val (truncated, remaining) = patchNote.body.formatHTML().truncateMarkdown(maxLength)

        title = patchNote.title
        color = DISCORD_GREEN

        description = "[Full patch notes](https://quiltmc.org/mc-patchnotes/#${patchNote.version})\n\n"
        description += truncated

        if (remaining > 0) {
            description += "\n\n[... $remaining more lines]"
        }

        thumbnail {
            url = "$BASE_URL${patchNote.image.url}"
        }

        footer {
            text = "URL: https://quiltmc.org/mc-patchnotes/#${patchNote.version}"
        }
    }

    private suspend fun MessageChannel.relay(patchNote: PatchNote, maxLength: Int = 1000) =
        createMessage { embed { patchNotes(patchNote, maxLength) } }

    @OptIn(KordPreview::class)
    class CheckArguments : Arguments() {
        val version by optionalString("version", "Specific version to get patch notes for")
    }
}
