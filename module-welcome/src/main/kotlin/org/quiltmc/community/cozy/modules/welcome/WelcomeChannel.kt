/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.welcome

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlException
import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.utils.deleteIgnoringNotFound
import com.kotlindiscord.kord.extensions.utils.hasNotStatus
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.create.allowedMentions
import dev.kord.rest.builder.message.modify.allowedMentions
import dev.kord.rest.request.RestRequestException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.decodeFromString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.quiltmc.community.cozy.modules.welcome.blocks.Block
import org.quiltmc.community.cozy.modules.welcome.config.WelcomeChannelConfig

public class WelcomeChannel(
    public val channel: GuildMessageChannel,
    public val url: String,
) : KoinComponent {
    private var blocks: MutableList<Block> = mutableListOf()
    private val messageMapping: MutableMap<Snowflake, Block> = mutableMapOf()

    private val config: WelcomeChannelConfig by inject()
    private val client = HttpClient()

    private lateinit var yaml: Yaml
    private var task: Task? = null

    public val scheduler: Scheduler = Scheduler()

    public suspend fun setup() {
        val taskDelay = config.getRefreshDelay()

        if (!::yaml.isInitialized) {
            yaml = Yaml(
                config.getSerializersModule(),
                YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property)
            )
        }

        task?.cancel()

        if (taskDelay != null) {
            task = scheduler.schedule(taskDelay, false) {
                populate()
            }
        }

        populate()

        task?.start()
    }

    public fun shutdown() {
        task?.cancel()
        scheduler.shutdown()
    }

    public suspend fun getBlocks(): List<Block> {
        try {
            val response = client.get(url).body<String>()

            return yaml.decodeFromString(response)
        } catch (e: ClientRequestException) {
            throw DiscordRelayedException("Failed to download the YAML file\n\n>>> $e")
        } catch (e: YamlException) {
            throw DiscordRelayedException("Failed to parse the given YAML\n\n>>> $e")
        }
    }

    public suspend fun populate() {
        task?.cancel()

        blocks = getBlocks().toMutableList()

        val messages = channel.withStrategy(EntitySupplyStrategy.rest)
            .messages
            .filter { it.type == MessageType.Default }
            .toList()
            .sortedBy { it.id.timestamp }

        if (messages.size > blocks.size) {
            messages.forEachIndexed { index, message ->
                val block = blocks.getOrNull(index)

                if (block != null) {
                    if (messageNeedsUpdate(message, block)) {
                        message.edit {
                            block.edit(this)

                            allowedMentions { }
                        }
                    }

                    messageMapping[message.id] = block
                } else {
                    message.delete()
                    messageMapping.remove(message.id)
                }
            }
        } else {
            blocks.forEachIndexed { index, block ->
                val message = messages.getOrNull(index)

                if (message != null) {
                    if (messageNeedsUpdate(message, block)) {
                        message.edit {
                            block.edit(this)

                            allowedMentions { }
                        }
                    }

                    messageMapping[message.id] = block
                } else {
                    val newMessage = channel.createMessage {
                        block.create(this)

                        allowedMentions { }
                    }

                    messageMapping[newMessage.id] = block
                }
            }
        }

        task?.start()
    }

    public suspend fun clear() {
        val messages = channel.withStrategy(EntitySupplyStrategy.rest)
            .messages
            .toList()
            .filter { it.type == MessageType.Default }

        try {
            channel.bulkDelete(messages.map { it.id })
        } catch (e: RestRequestException) {
            if (e.hasNotStatus(HttpStatusCode.NotFound)) {
                messages.forEach { it.deleteIgnoringNotFound() }
            }
        }
    }

    private suspend fun messageNeedsUpdate(message: Message, block: Block): Boolean {
        val builder = UserMessageCreateBuilder()

        block.create(builder)

        return !builder.isSimilar(message)
    }
}
