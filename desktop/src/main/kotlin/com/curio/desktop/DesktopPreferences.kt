package com.curio.desktop

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * v27t — desktop preferences store: a tiny pretty-printed JSON file at
 * `~/.curio/prefs.json` (Gson, the same serializer the topic loader uses).
 * The shell reads it at startup and writes on change, so the selected lane,
 * landed topic, window geometry and theme survive restarts.
 *
 * Threading: all access happens on the UI thread (shell settings + window
 * geometry); writes are synchronous but tiny (a handful of strings), so no
 * locking is needed yet.
 */
object DesktopPreferences {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dir = File(System.getProperty("user.home"), ".curio")
    private val file = File(dir, "prefs.json")
    private val data: MutableMap<String, String> = linkedMapOf()

    init {
        // Best-effort load: a corrupt/missing file falls back to defaults.
        runCatching {
            if (file.isFile) {
                val raw = gson.fromJson(file.readText(), Map::class.java)
                if (raw != null) {
                    raw.forEach { (k, v) ->
                        if (k is String && v is String) data[k] = v
                    }
                }
            }
        }
    }

    fun get(key: String, default: String): String = data[key] ?: default

    fun set(key: String, value: String) {
        data[key] = value
        runCatching {
            dir.mkdirs()
            file.writeText(gson.toJson(data))
        }
    }

    fun getBoolean(key: String, default: Boolean): Boolean =
        get(key, if (default) "1" else "0") == "1"

    fun setBoolean(key: String, value: Boolean) = set(key, if (value) "1" else "0")

    fun getInt(key: String, default: Int): Int =
        get(key, default.toString()).toIntOrNull() ?: default

    fun setInt(key: String, value: Int) = set(key, value.toString())
}
