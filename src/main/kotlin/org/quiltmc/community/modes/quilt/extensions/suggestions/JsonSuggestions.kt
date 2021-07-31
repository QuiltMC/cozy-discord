package org.quiltmc.community.modes.quilt.extensions.suggestions

import com.kotlindiscord.kord.extensions.utils.env
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class JsonSuggestions : SuggestionsData {
    private val json = Json

    private val root = (env("DATA_ROOT") ?: "/data") + "/suggestions"
    private val path = "$root/{id}.json"

    private val suggestionCache: MutableMap<String, Suggestion> = mutableMapOf()

    override fun get(id: String): Suggestion? {
        if (suggestionCache.containsKey(id)) {
            return suggestionCache[id]
        }

        val file = getFile(id) ?: return null
        val suggestion = json.decodeFromString<Suggestion>(file.readText())

        suggestionCache[id] = suggestion

        return suggestion
    }

    override fun add(id: String, suggestion: Suggestion): Boolean {
        val existing = get(id)

        if (existing != null) {
            return false
        }

        suggestionCache[id] = suggestion
        save(id)

        return true
    }

    override fun load(): Int {
        if (suggestionCache.isNotEmpty()) {
            save()
            suggestionCache.clear()
        }

        val rootPath = Path.of(root)

        if (!rootPath.exists()) {
            rootPath.createDirectories()
//            return 0
        }

        return 0

//        rootPath.toFile().listFiles()!!.filter {
//            val name = it.name.split(File.separatorChar).last()
//
//            name.matches("\\d+\\.json".toRegex())
//        }.forEach {
//            get(it.name.split(File.separatorChar).last().split(".").first())
//                ?: error("Failed to load suggestion: ${it.name}")
//        }
//
//        return suggestionCache.size
    }

    override fun save(): Boolean =
        suggestionCache.keys.map { id ->
            save(id)
        }.all { it }

    override fun save(id: String): Boolean {
        val suggestion = get(id) ?: return false
        val pathObj = Path.of(path.replace("{id}", id))

        if (!pathObj.exists()) {
            pathObj.createFile()
        }

        pathObj.writeText(json.encodeToString(suggestion))

        return true
    }

    override fun save(suggestion: Suggestion): Boolean {
        val id = suggestion.id

        if (suggestionCache[id] == null) {
            suggestionCache[id] = suggestion
        }

        val pathObj = Path.of(path.replace("{id}", id))

        if (!pathObj.exists()) {
            pathObj.createFile()
        }

        pathObj.writeText(json.encodeToString(suggestion))

        return true
    }

    fun getFile(id: String): File? {
        val pathObj = Path.of(path.replace("{id}", id))

        if (pathObj.exists()) {
            return pathObj.toFile()
        }

        return null
    }
}
