
package com.example.timetable

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalBrain(
    private val context: Context
) {

    private var modelPath: String? = null

    var loaded: Boolean = false
        private set

    fun setModel(
        path: String
    ) {

        modelPath = path
        loaded = true
    }

    fun unload() {

        modelPath = null
        loaded = false
    }

    fun generate(
        prompt: String
    ): Flow<String> = flow {

        /*
         * This is the boundary between our Android application
         * and llama.cpp.
         *
         * The official llama.android module exposes the actual
         * inference engine. Wire its AiChat / InferenceEngine
         * instance here after copying the official module into
         * the project.
         */

        if (!loaded) {

            emit(
                "Please load a GGUF model first."
            )

            return@flow
        }

        /*
         * The exact API surface of the official binding can change
         * with llama.cpp revisions. Keeping it behind this class
         * prevents the rest of our application from depending on
         * native llama.cpp details.
         */

        emit(
            "Local model loaded. Connect the selected GGUF to the llama.android inference engine."
        )
    }
}
