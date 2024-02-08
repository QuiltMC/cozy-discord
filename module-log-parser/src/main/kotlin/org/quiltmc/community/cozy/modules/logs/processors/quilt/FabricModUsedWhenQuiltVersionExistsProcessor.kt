/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.quiltmc.community.cozy.modules.logs.processors.quilt

import dev.kord.core.event.Event
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import org.quiltmc.community.cozy.modules.logs.Version
import org.quiltmc.community.cozy.modules.logs.data.LoaderType
import org.quiltmc.community.cozy.modules.logs.data.Log
import org.quiltmc.community.cozy.modules.logs.data.Order
import org.quiltmc.community.cozy.modules.logs.types.LogProcessor
import java.util.*

private const val MODRINTH_API_BASE = "https://api.modrinth.com/v2"

public class FabricModUsedWhenQuiltVersionExistsProcessor : LogProcessor() {

	override val identifier: String = "fabric-mod-used-when-quilt-version-exists"
	override val order: Order = Order.Default
	private val json = Json {
		ignoreUnknownKeys = true
	}

	override suspend fun predicate(log: Log, event: Event): Boolean = log.getLoaderVersion(LoaderType.Quilt) != null

	override suspend fun process(log: Log) {
		val mcVersion = log.minecraftVersion?.string ?: log.getMod("minecraft")?.version?.string ?: return

		val hashToMod = log.getMods().values.filter {
			it.type?.lowercase(Locale.getDefault()) == "fabric" && it.hash != null
		}.associateBy { it.hash!! }
		if (hashToMod.isEmpty()) return

		val hashesRequest = client.post("$MODRINTH_API_BASE/version_files") {
			setBody(json.encodeToString(HashLookupParam(hashToMod.keys.toList(), "sha1")))
			contentType(ContentType.Application.Json)
		}
		if (hashesRequest.status != HttpStatusCode.OK) return

		val projectIdToNoneQuiltVersions = json.decodeFromJsonElement<Map<String, Version>>(hashesRequest.body())
			.filter { !it.value.loaders.contains("quilt") }
			.map { (hash, version) -> version.projectId to hashToMod[hash] }
			.toMap()
		if (projectIdToNoneQuiltVersions.isEmpty()) return

		val projectsRequest = client.get("$MODRINTH_API_BASE/projects") {
			url {
				parameters.append("ids", json.encodeToString(projectIdToNoneQuiltVersions.keys.toList()))
			}
		}
		if (projectsRequest.status != HttpStatusCode.OK) return

		val body = projectsRequest.body<JsonArray>()
		val potentialBetterVersionCandidates = json.decodeFromJsonElement<List<Project>>(body)
			.filter { it.gameVersions.contains(mcVersion) && it.loaders.contains("quilt") }
			.flatMap { it.versions }
		if (potentialBetterVersionCandidates.isEmpty()) return

		val versionsRequest = client.get("$MODRINTH_API_BASE/versions") {
			url {
				parameters.append("ids", json.encodeToString(potentialBetterVersionCandidates))
			}
		}
		if (versionsRequest.status != HttpStatusCode.OK) return

		val modIdToProjectVersion = mutableMapOf<String, MutableList<Version>>()
		json.decodeFromJsonElement<List<Version>>(versionsRequest.body())
			.filter { it.loaders.contains("quilt") && it.gameVersions.contains(mcVersion) }
			.forEach {
				val mod = projectIdToNoneQuiltVersions[it.projectId]!!
				val version = Version(removeLoaderIdentifier(it.versionNumber))
				val oldVersion = Version(removeLoaderIdentifier(mod.version.string))
				if (version >= oldVersion) {
					modIdToProjectVersion.getOrPut(mod.id) { mutableListOf<Version>() }.add(it)
				}
			}
		if (modIdToProjectVersion.isEmpty()) return

		val modsWithNewerVersions = mutableListOf<String>()
		for ((modid, versions) in modIdToProjectVersion) {
			val mod = log.getMod(modid)!!
			if (versions.size == 1) {
				val version = versions[0]
				val oldMod = projectIdToNoneQuiltVersions[version.projectId]!!
				modsWithNewerVersions.add(
					"`${mod.id}`: Switch from ${oldMod.version.string} to " +
					"[${version.versionNumber} (Modrinth)]" +
					"(https://modrinth.com/mod/${version.projectId}/version/${versions[0].id})"
				)
			} else {
				modsWithNewerVersions.add(
					"`${mod.id}`: See [Modrinth](https://modrinth.com/mod/" +
					"${versions[0].projectId}/versions?l=quilt&v=$mcVersion)"
				)
			}
		}
		log.addMessage(
			buildString {
				appendLine(
					"The following fabric mods are marked as fabric only on Modrinth, " +
					"but newer or alternative versions with explicit quilt support exist:"
				)
				for (mod in modsWithNewerVersions) {
					appendLine(" - $mod")
				}
			}
		)
	}

	private fun removeLoaderIdentifier(versionNumber: String): String = versionNumber.lowercase()
		.replace(Regex("quilt[-+ ]"), "")
		.replace(Regex("fabric[-+ ]"), "")
		.replace(Regex("[-+ ]quilt"), "")
		.replace(Regex("[-+ ]fabric"), "")
		.replace("quilt", "")
		.replace("fabric", "")

	@Serializable
	public data class Project(
		public val versions: List<String>,
		@SerialName("game_versions") public val gameVersions: List<String>,
		public val loaders: List<String>
	)

	@Serializable
	public data class HashLookupParam(public val hashes: List<String>, public val algorithm: String)

	@Serializable
	public data class Version(
		public val loaders: List<String>,
		@SerialName("project_id") public val projectId: String,
		@SerialName("version_number") public val versionNumber: String,
		@SerialName("game_versions") public val gameVersions: List<String>,
		public val id: String
	)
}
