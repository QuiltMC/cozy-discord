/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.quiltmc.community.cozy.modules.logs.Version
import org.quiltmc.community.cozy.modules.logs.config.SimpleLogParserConfig
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import java.net.URL

class LogParseTests {
	private val config = SimpleLogParserConfig {}
	private val logger = KotlinLogging.logger {}

	private suspend fun loadLog(
		url: URL,
		parseAbortStatus: Boolean = false,
		processAbortStatus: Boolean = false
	): Log {
		val log = Log()
		log.content = url.readText()
		log.url = url

		for (parser in config.getParsers()) {
			try {
				parser.process(log)
			} catch (e: Exception) {
				logger.warn(e) { "Unexpected error while parsing logs" }
			}

			assertEquals(parseAbortStatus, log.aborted) { "Unexpected log abort during parsing" }
		}

		for (processor in config.getProcessors()) {
			try {
				processor.process(log)
			} catch (e: Exception) {
				logger.warn(e) { "Unexpected error while processing logs" }
			}

			assertEquals(processAbortStatus, log.aborted) { "Unexpected log abort during processing" }
		}

		return log
	}

	@Test
	fun `Test Quilt crash log`() {
		if (System.getenv("CI") != null) {
			logger.error { "Weird bug with CI testing, please notify if reproducible" }
			return
		}

		runBlocking {
			val file = ClassLoader.getSystemResource("quilt-crash.txt")
			val log = loadLog(file)

			assertEquals(167, log.getMods().size)
			assertEquals(Version("1.19.3"), log.minecraftVersion)
			assertEquals(mapOf(LoaderType.Quilt to Version("0.18.10")), log.getLoaders())
		}
	}
}
