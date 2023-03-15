/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import ch.qos.logback.core.joran.spi.ConsoleTarget
import org.quiltmc.community.DiscordLogAppender

def environment = System.getenv().getOrDefault("ENVIRONMENT", "prod")
def logUrl = System.getenv().getOrDefault("DISCORD_LOGGER_URL", null)

def defaultLevel = INFO
def defaultTarget = ConsoleTarget.SystemErr

if (environment == "dev") {
	defaultLevel = DEBUG
	defaultTarget = ConsoleTarget.SystemOut

	// Silence warning about missing native PRNG
	logger("io.ktor.util.random", ERROR)
}

appender("CONSOLE", ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = "%boldGreen(%d{yyyy-MM-dd}) %boldYellow(%d{HH:mm:ss}) %gray(|) %highlight(%5level) %gray(|) %boldMagenta(%40.40logger{40}) %gray(|) %msg%n"

		withJansi = true
	}

	target = defaultTarget
}

if (logUrl != null) {
	appender("DISCORD_ALL", DiscordLogAppender) {
		url = System.getenv("DISCORD_LOGGER_URL")
	}

	appender("DISCORD_WARN", DiscordLogAppender) {
		level = WARN
		url = System.getenv("DISCORD_LOGGER_URL")
	}

	logger(
			"org.quiltmc.community.modes.quilt.extensions.messagelog.MessageLogExtension",
			TRACE,
			["DISCORD_ALL"]
	)

	logger(
			"org.quiltmc.community.AppKt.setupQuilt.extLogParser.predicate",
			DEBUG,
			["DISCORD_WARN"]
	)
}

root(defaultLevel, ["CONSOLE"])
