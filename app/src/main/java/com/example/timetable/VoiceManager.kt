package com.example.timetable

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onListening: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {

    private var recognizer:
            SpeechRecognizer? = null

    fun start() {

        if (
            !SpeechRecognizer
                .isRecognitionAvailable(context)
        ) {

            onError(
                "Speech recognition is unavailable."
            )

            return
        }

        stop()

        recognizer =
            SpeechRecognizer
                .createSpeechRecognizer(context)

        recognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    onListening(true)
                }

                override fun onBeginningOfSpeech() {
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }

                override fun onEndOfSpeech() {

                    onListening(false)
                }

                override fun onError(
                    error: Int
                ) {

                    onListening(false)

                    onError(
                        "Speech recognition error: $error"
                    )
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    onListening(false)

                    val resultsList =
                        results?.getStringArrayList(
                            SpeechRecognizer
                                .RESULTS_RECOGNITION
                        )

                    val text =
                        resultsList
                            ?.firstOrNull()
                            ?.trim()

                    if (!text.isNullOrBlank()) {

                        onResult(text)
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
            }

        recognizer?.startListening(intent)
    }

    fun stop() {

        recognizer?.stopListening()
        recognizer?.cancel()

        onListening(false)
    }

    fun destroy() {

        recognizer?.destroy()

        recognizer = null
    }
}
