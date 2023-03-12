/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import ch.qos.logback.core.joran.spi.ConsoleTarget

def environment = System.getenv().getOrDefault("ENVIRONMENT", "prod")

def defaultLevel = DEBUG
def defaultTarget = ConsoleTarget.SystemErr

if (environment == "dev") {
	defaultLevel = TRACE
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

root(defaultLevel, ["CONSOLE"])
