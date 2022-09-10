/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.modes.quilt.extensions

import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.quiltmc.community.GUILDS
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister

@Suppress("UnusedPrivateMember")
class JavadocExtension : Extension() {
	override val name: String = "javadoc"

	private val commonProjects = listOf("qsl", "quilt-mappings", "quilt-loader")

	private val client = HttpClient {
		expectSuccess = false
	}

	private val xmlSerializer: Serializer = Persister()

	override suspend fun setup() {
		GUILDS.forEach { guildId ->
			publicSlashCommand {
				name = "javadoc"
				description = "Get Javadocs for common Quilt Projects"

				allowInDms = true

				guild(guildId)

				action {
					val urls = commonProjects.map {
						val version = getLatestVersion(it)
						val url = getJavadoc(it, version)
						"[$it ($version)]($url/)"
					}

					respond {
						embed {
							title = "Latest Javadoc Versions"
							description = urls.joinToString("\n")
						}
					}
				}

				publicSubCommand {
					name = "all"
					description = "Get Javadocs for all Quilt Projects"

					allowInDms = true

					guild(guildId)

					action {
						val urls = getProjects().map {
							val version = getLatestVersion(it)
							val url = getJavadoc(it, version)
							"[$it ($version)]($url/)"
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
	}

	// TODO: Make a proper API for the artifacts in the maven
	private fun getProjects(): Array<String> =
		arrayOf("qsl", "quilt-mappings", "quilt-loader", "quilt-config", "quilt-json5")

	private suspend fun getVersions(project: String): List<String> =
		getVersionMetadata(project).versioning.versions

	private suspend fun getLatestVersion(project: String): String =
		getVersionMetadata(project).versioning.latest

	private suspend fun getVersionMetadata(project: String): MavenMetadata =
		xmlSerializer.read(
			MavenMetadata::class.java,
			client.get("${getProjectUrl(project)}/maven-metadata.xml").body<String>()
		)

	private fun getProjectUrl(project: String): String =
		"https://maven.quiltmc.org/repository/release/org/quiltmc/$project"

	private suspend fun getJavadoc(project: String, version: String): String {
		var url = "${getProjectUrl(project)}/$version/$project-$version-javadoc.jar"

		if (client.get(url).status == HttpStatusCode.NotFound) {
			url = "${getProjectUrl(project)}/$version/$project-$version-fat-javadoc.jar"
		}

		return url
	}

	@Root(name = "metadata", strict = false)
	class MavenMetadata {
		@field:Element(name = "versioning", required = true)
		lateinit var versioning: MavenVersioning
	}

	@Root(name = "versioning", strict = false)
	class MavenVersioning {
		@field:Element(name = "latest", required = true)
		lateinit var latest: String

		@field:ElementList(name = "versions", required = true)
		lateinit var versions: List<String>
	}
}
