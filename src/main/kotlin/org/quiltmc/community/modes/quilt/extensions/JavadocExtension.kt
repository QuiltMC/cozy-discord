/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.quiltmc.community.GUILDS
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.core.Persister
import java.net.URL

class JavadocExtension : Extension() {
	override val name: String = "javadoc"

	private val COMMON_PROJECTS = listOf("qsl", "quilt-mappings", "quilt-loader")

	override suspend fun setup() {
		GUILDS.forEach { guildId ->
			publicSlashCommand {
				name = "javadoc"
				description = "Get Javadocs for Quilt Projects"

				allowInDms = true

				guild(guildId)

				action {
					val urls = COMMON_PROJECTS.map {
						"[$it](https://maven.quiltmc.org/repository/release/org/quiltmc/${it}/${
							getLatestVersion(it)
						}/${it}-${getLatestVersion(it)}-javadoc.jar)"
					}

					respond {
						embed {
							title = "Latest Javadoc Versions"
							description = urls.joinToString("\n")
						}
					}
				}
			}
		}
	}

	private fun getProjects(): Array<String> {
		// TODO: Make a proper API for the artifacts in the maven
		return arrayOf("qsl", "quilt-mappings", "quilt-loader", "quilt-config", "quilt-json5")
	}

	private suspend fun getVersions(project: String): List<String> {
		return Persister().read(MavenMetadata::class.java,
			withContext(Dispatchers.IO) {
				URL("https://maven.quiltmc.org/repository/release/org/quiltmc/${project}/maven-metadata.xml").openStream()
			}).versioning.versions
	}

	private suspend fun getLatestVersion(project: String): String {
		return Persister().read(MavenMetadata::class.java,
			withContext(Dispatchers.IO) {
				URL("https://maven.quiltmc.org/repository/release/org/quiltmc/${project}/maven-metadata.xml").openStream()
			}).versioning.latest
	}

	@Root(name = "metadata", strict = false)
	public class MavenMetadata {
		@field:Element(name = "versioning", required = true)
		lateinit var versioning: MavenVersioning
	}

	@Root(name = "versioning", strict = false)
	public class MavenVersioning {
		@field:Element(name = "latest", required = true)
		lateinit var latest: String

		@field:ElementList(name = "versions", required = true)
		lateinit var versions: List<String>
	}
}
