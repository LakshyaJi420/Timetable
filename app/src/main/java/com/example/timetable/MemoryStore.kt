package com.example.timetable

import android.content.Context
import org.json.JSONArray

class MemoryStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "neural_memory",
            Context.MODE_PRIVATE
        )

    fun add(message: String) {

        if (message.isBlank()) return

        val memories = getAll().toMutableList()

        memories.add(message)

        while (memories.size > 100) {
            memories.removeAt(0)
        }

        val array = JSONArray()

        memories.forEach {
            array.put(it)
        }

        preferences
            .edit()
            .putString(
                "memories",
                array.toString()
            )
            .apply()
    }

    fun getAll(): List<String> {

        val raw =
            preferences.getString(
                "memories",
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (i in 0 until array.length()) {
                    add(array.getString(i))
                }
            }

        } catch (
            e: Exception
        ) {

            emptyList()
        }
    }

    fun clear() {

        preferences
            .edit()
            .remove("memories")
            .apply()
    }

    fun context(): String {

        return getAll()
            .takeLast(20)
            .joinToString("\n") {
                "- $it"
            }
    }
}
