package com.example.timetable

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val fromUser: Boolean
)

class NeuralViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val memory =
        MemoryStore(application)

    private val brain =
        LocalBrain(application)

    private val _messages =
        MutableStateFlow<List<ChatMessage>>(
            emptyList()
        )

    val messages:
            StateFlow<List<ChatMessage>> =
        _messages.asStateFlow()

    private val _status =
        MutableStateFlow(
            "No model loaded"
        )

    val status:
            StateFlow<String> =
        _status.asStateFlow()

    private val _thinking =
        MutableStateFlow(false)

    val thinking:
            StateFlow<Boolean> =
        _thinking.asStateFlow()

    fun loadModel(
        path: String
    ) {

        try {

            brain.setModel(path)

            _status.value =
                "Local brain ready"

        } catch (e: Exception) {

            _status.value =
                "Model error: ${e.message}"
        }
    }

    fun send(
        text: String,
        speak: (String) -> Unit
    ) {

        if (text.isBlank()) return

        addMessage(
            ChatMessage(
                text = text,
                fromUser = true
            )
        )

        memory.add(
            "User: $text"
        )

        _thinking.value = true
        _status.value = "Thinking..."

        viewModelScope.launch {

            val prompt =
                buildPrompt(text)

            val result =
                StringBuilder()

            brain.generate(prompt)
                .collect { token ->

                    result.append(token)

                    /*
                     * Later we can update the UI token-by-token
                     * here for true streaming output.
                     */
                }

            val response =
                result.toString().trim()

            addMessage(
                ChatMessage(
                    text = response,
                    fromUser = false
                )
            )

            memory.add(
                "Assistant: $response"
            )

            _thinking.value = false
            _status.value = "Ready"

            if (response.isNotBlank()) {
                speak(response)
            }
        }
    }

    private fun buildPrompt(
        question: String
    ): String {

        val history =
            _messages.value
                .takeLast(12)
                .joinToString("\n") {

                    if (it.fromUser) {
                        "USER: ${it.text}"
                    } else {
                        "ASSISTANT: ${it.text}"
                    }
                }

        val memories =
            memory.context()

        return """
            You are Neural, a private AI assistant
            running locally on an Android phone.

            You are helpful, calm, intelligent and concise.

            Do not claim to have performed actions
            that you did not actually perform.

            Persistent memories:
            $memories

            Recent conversation:
            $history

            Latest user request:
            $question

            Answer naturally.

            ASSISTANT:
        """.trimIndent()
    }

    private fun addMessage(
        message: ChatMessage
    ) {

        _messages.value =
            _messages.value + message
    }

    fun clearConversation() {

        _messages.value =
            emptyList()
    }

    fun clearMemory() {

        memory.clear()
    }
}
