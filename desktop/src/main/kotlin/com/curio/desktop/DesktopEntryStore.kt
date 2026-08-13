package com.curio.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/** One saved discovery in the desktop Cabinet (mirrors the Android CurioEntry). */
data class DesktopEntry(
    val id: String,
    val slug: String,
    val topicId: String,
    val savedAt: Long
)

/**
 * v27t — desktop Cabinet persistence: the user's saved discoveries live in a
 * pretty-printed JSON file at `~/.curio/entries.json` (Gson). The list is
 * backed by Compose state so the Cabinet screen recomposes the moment a
 * topic is saved or removed.
 */
object DesktopEntryStore {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dir = File(System.getProperty("user.home"), ".curio")
    private val file = File(dir, "entries.json")

    /** Newest first. Reactive — screens read this directly. */
    var entries by mutableStateOf<List<DesktopEntry>>(emptyList())
        private set

    init {
        runCatching {
            if (file.isFile) {
                val loaded = gson.fromJson(file.readText(), Array<DesktopEntry>::class.java)
                if (loaded != null) entries = loaded.toList()
            }
        }
    }

    /** Saves a topic (prepends so the newest entry is first) and persists. */
    fun add(slug: String, topicId: String) {
        val entry = DesktopEntry(
            id = "$topicId-${System.currentTimeMillis()}",
            slug = slug,
            topicId = topicId,
            savedAt = System.currentTimeMillis()
        )
        entries = listOf(entry) + entries
        save()
    }

    fun remove(id: String) {
        entries = entries.filterNot { it.id == id }
        save()
    }

    fun removeAll() {
        entries = emptyList()
        save()
    }

    private fun save() {
        runCatching {
            dir.mkdirs()
            file.writeText(gson.toJson(entries))
        }
    }
}
