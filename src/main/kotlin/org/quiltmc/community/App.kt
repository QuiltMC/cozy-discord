/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:OptIn(PrivilegedIntent::class)

package org.quiltmc.community

import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.gateway.ALL
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.checks.guildFor
import dev.kordex.core.utils.envOrNull
import dev.kordex.core.utils.getKoin
import dev.kordex.modules.func.phishing.DetectionAction
import dev.kordex.modules.func.phishing.extPhishing
import dev.kordex.modules.func.tags.tags
import dev.kordex.modules.func.welcome.welcomeChannel
import dev.kordex.modules.pluralkit.extPluralKit
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.lastOrNull
import org.quiltmc.community.cozy.modules.ama.extAma
import org.quiltmc.community.cozy.modules.logs.extLogParser
import org.quiltmc.community.cozy.modules.logs.processors.PiracyProcessor
import org.quiltmc.community.cozy.modules.logs.processors.ProblematicLauncherProcessor
import org.quiltmc.community.cozy.modules.moderation.moderation
import org.quiltmc.community.cozy.modules.rolesync.rolesync
import org.quiltmc.community.database.collections.AmaConfigCollection
import org.quiltmc.community.database.collections.TagsCollection
import org.quiltmc.community.database.collections.WelcomeChannelCollection
import org.quiltmc.community.logs.NonQuiltLoaderProcessor
import org.quiltmc.community.logs.RuleBreakingModProcessor
import org.quiltmc.community.modes.collab.extensions.LookupExtension
import org.quiltmc.community.modes.quilt.extensions.*
import org.quiltmc.community.modes.quilt.extensions.filtering.FilterExtension
import org.quiltmc.community.modes.quilt.extensions.github.GithubExtension
import org.quiltmc.community.modes.quilt.extensions.messagelog.MessageLogExtension
import org.quiltmc.community.modes.quilt.extensions.minecraft.MinecraftExtension
import org.quiltmc.community.modes.quilt.extensions.modhostverify.ModHostingVerificationExtension
import org.quiltmc.community.modes.quilt.extensions.settings.SettingsExtension
import kotlin.time.Duration.Companion.minutes

val MODE = envOrNull("MODE")?.lowercase() ?: "quilt"
val ENVIRONMENT = envOrNull("ENVIRONMENT")?.lowercase() ?: "production"

suspend fun setupCollab() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database()

	about {
		name = "Cozy: Collab"
		description = "Quilt's Discord bot, Community Collab edition."
	}

	extensions {
		sentry {
			distribution = "collab"
		}
	}
}

suspend fun setupDev() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database()

	about {
		name = "Cozy: Dev Tools"
		description = "Quilt's Discord bot, Dev Tools edition.\n\n" +
			"Once provided mappings commands, but you should use the Allium Discord bot or " +
			"[Linkie Web](https://linkie.shedaniel.dev/) going forward."
	}

	extensions {
		if (GITHUB_TOKEN != null) {
			add(::GithubExtension)
		}

		sentry {
			distribution = "dev"
		}
	}
}

suspend fun setupQuilt() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database(true)
	settings()

	about {
		name = "Cozy: Community"
		description = "Quilt's Discord bot, Community edition.\n\n" +
			"Provides a ton of commands and other utilities, to help staff with moderation and provide users with " +
			"day-to-day features on the main Discord server."
	}

	chatCommands {
		defaultPrefix = "%"
		enabled = true
	}

	intents {
		+Intents.ALL
	}

	members {
		all()

		fillPresences = true
	}

	extensions {
		add(::FilterExtension)
		add(::ForumExtension)
		add(::LookupExtension)
		add(::MessageLogExtension)
		add(::MinecraftExtension)
		add(::ModHostingVerificationExtension)
		add(::SettingsExtension)
		add(::ShowcaseExtension)
		add(::SyncExtension)
		add(::UtilityExtension)

		extPluralKit()

		extAma(getKoin().get<AmaConfigCollection>())

		extLogParser {
			// Bundled non-default processors
			processor(PiracyProcessor())
			processor(ProblematicLauncherProcessor())

			// Quilt-specific processors
			processor(NonQuiltLoaderProcessor())
			processor(RuleBreakingModProcessor())

			globalPredicate { event ->
				val guild = guildFor(event)

				guild?.id != COLLAB_GUILD
			}
		}

		help {
			enableBundledExtension = true
		}

		welcomeChannel(getKoin().get<WelcomeChannelCollection>()) {
			staffCommandCheck {
				hasBaseModeratorRole()
			}

			getLogChannel { _, guild ->
				guild.channels
					.filterIsInstance<GuildMessageChannel>()
					.filter { it.name == "cozy-logs" }
					.lastOrNull()
			}

			refreshDuration = 5.minutes
		}

		tags(getKoin().get<TagsCollection>()) {
			loggingChannelName = "cozy-logs"

			userCommandCheck {
				inQuiltGuild()
			}

			staffCommandCheck {
				hasBaseModeratorRole()
			}
		}

		extPhishing {
			detectionAction = DetectionAction.Kick
			logChannelName = "cozy-logs"
			requiredCommandPermission = null

			check { inQuiltGuild() }
			check { notHasBaseModeratorRole() }
			check { notInCollab() }
		}

		moderation {
			loggingChannelName = "cozy-logs"
			verifiedRoleName = "Verified"

			commandCheck { inQuiltGuild() }
			commandCheck { hasBaseModeratorRole() }
		}

		rolesync {
			roleToSync(  // Devs: Community -> Toolchain
				COMMUNITY_DEVELOPER_ROLE,
				TOOLCHAIN_DEVELOPER_ROLE,
			)

			roleToSync(  // Collabs: Collab -> Toolchain
				COLLAB_VERIFIED_ROLE,
				TOOLCHAIN_COLLAB_ROLE
			)

			roleToSync(  // Collabs: Collab -> Community
				COLLAB_VERIFIED_ROLE,
				COMMUNITY_COLLAB_ROLE
			)

			commandCheck { inQuiltGuild() }
			commandCheck { hasBaseModeratorRole() }
		}

		sentry {
			distribution = "community"
		}
	}
}

suspend fun setupShowcase() = ExtensibleBot(DISCORD_TOKEN) {
	common()
	database()
	settings()

	about {
		name = "Cozy: Showcase"
		description = "Quilt's Discord bot, Showcase edition.\n\n" +
			"This bot is currently in development, but someday we hope it'll let you post in the showcase " +
			"channels from your project servers."
	}

	extensions {
		sentry {
			distribution = "showcase"
		}
	}
}

suspend fun main() {
	val bot = when (MODE) {
		"collab" -> setupCollab()
		"dev" -> setupDev()
		"quilt" -> setupQuilt()
		"showcase" -> setupShowcase()

		else -> error("Invalid mode: $MODE")
	}

	bot.start()
}
