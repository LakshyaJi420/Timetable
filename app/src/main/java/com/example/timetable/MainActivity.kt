package com.example.timetable

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var tts: TextToSpeech

    override fun onCreate(
        savedInstanceState: android.os.Bundle?
    ) {

        super.onCreate(savedInstanceState)

        tts =
            TextToSpeech(
                this
            ) { result ->

                if (
                    result ==
                    TextToSpeech.SUCCESS
                ) {

                    tts.language =
                        Locale.getDefault()

                    tts.setSpeechRate(
                        0.95f
                    )
                }
            }

        setContent {

            NeuralScreen(
                speak = {
                    speak(it)
                }
            )
        }
    }

    private fun speak(
        text: String
    ) {

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "neural"
        )
    }

    override fun onDestroy() {

        tts.stop()
        tts.shutdown()

        super.onDestroy()
    }
}

@Composable
fun NeuralScreen(
    speak: (String) -> Unit,
    vm: NeuralViewModel = viewModel()
) {

    val context =
        LocalContext.current

    val messages by
        vm.messages.collectAsState()

    val status by
        vm.status.collectAsState()

    val thinking by
        vm.thinking.collectAsState()

    var text by
        remember {
            mutableStateOf("")
        }

    var listening by
        remember {
            mutableStateOf(false)
        }

    val voiceManager =
        remember {

            VoiceManager(
                context = context,

                onResult = { result ->

                    text = ""

                    vm.send(
                        result,
                        speak
                    )
                },

                onListening = {
                    listening = it
                },

                onError = {
                    Toast
                        .makeText(
                            context,
                            it,
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            )
        }

    DisposableEffect(Unit) {

        onDispose {
            voiceManager.destroy()
        }
    }

    val microphonePermission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (!granted) {

                Toast.makeText(
                    context,
                    "Microphone permission is required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    val modelPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                try {

                    val input =
                        context.contentResolver
                            .openInputStream(uri)

                    if (input == null) {
                        return@rememberLauncherForActivityResult
                    }

                    val modelFile =
                        java.io.File(
                            context.filesDir,
                            "selected-model.gguf"
                        )

                    input.use { source ->

                        modelFile
                            .outputStream()
                            .use { destination ->

                                source.copyTo(
                                    destination
                                )
                            }
                    }

                    vm.loadModel(
                        modelFile.absolutePath
                    )

                } catch (e: Exception) {

                    Toast.makeText(
                        context,
                        "Could not import model: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary =
                    Color(0xFF6C7BFF),

                secondary =
                    Color(0xFF9C8CFF),

                background =
                    Color(0xFF07090E),

                surface =
                    Color(0xFF10131A)
            )
    ) {

        Surface(
            modifier =
                Modifier.fillMaxSize(),

            color =
                Color(0xFF07090E)
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
            ) {

                TopBar(
                    status = status,

                    onModel = {
                        modelPicker.launch(
                            arrayOf(
                                "application/octet-stream",
                                "application/*"
                            )
                        )
                    },

                    onClear = {
                        vm.clearConversation()
                    }
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                ChatList(
                    messages = messages,

                    modifier =
                        Modifier.weight(1f)
                )

                if (thinking) {

                    Text(
                        text =
                            "Neural is thinking...",

                        color =
                            Color(0xFF8794FF),

                        modifier =
                            Modifier.padding(8.dp)
                    )
                }

                InputBar(
                    value = text,

                    onValueChange = {
                        text = it
                    },

                    listening = listening,

                    onMic = {

                        if (
                            ContextCompat
                                .checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {

                            microphonePermission
                                .launch(
                                    Manifest.permission.RECORD_AUDIO
                                )

                        } else {

                            if (listening) {
                                voiceManager.stop()
                            } else {
                                voiceManager.start()
                            }
                        }
                    },

                    onSend = {

                        val message =
                            text.trim()

                        if (
                            message.isNotBlank()
                        ) {

                            text = ""

                            vm.send(
                                message,
                                speak
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    status: String,
    onModel: () -> Unit,
    onClear: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Color(0xFF5865F2)
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Default.VolumeUp,

                contentDescription =
                    null,

                tint =
                    Color.White
            )
        }

        Spacer(
            Modifier.width(12.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = "NEURAL",

                fontWeight =
                    FontWeight.Bold,

                style =
                    MaterialTheme.typography
                        .titleLarge
            )

            Text(
                text = status,

                color =
                    Color(0xFF8D95A5),

                style =
                    MaterialTheme.typography
                        .bodySmall
            )
        }

        IconButton(
            onClick = onModel
        ) {

            Icon(
                Icons.Default.FolderOpen,
                contentDescription =
                    "Load GGUF model"
            )
        }

        IconButton(
            onClick = onClear
        ) {

            Icon(
                Icons.Default.Delete,
                contentDescription =
                    "Clear conversation"
            )
        }
    }
}

@Composable
private fun ChatList(
    messages: List<ChatMessage>,
    modifier: Modifier
) {

    LazyColumn(
        modifier = modifier,

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        items(
            messages
        ) { message ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    if (message.fromUser) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
            ) {

                Surface(
                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    color =
                        if (
                            message.fromUser
                        ) {
                            Color(0xFF3545A5)
                        } else {
                            Color(0xFF151922)
                        }
                ) {

                    Text(
                        text =
                            message.text,

                        color =
                            Color.White,

                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    listening: Boolean,
    onMic: () -> Unit,
    onSend: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        OutlinedTextField(
            value = value,

            onValueChange =
                onValueChange,

            modifier =
                Modifier.weight(1f),

            placeholder = {
                Text(
                    "Talk to Neural..."
                )
            },

            maxLines = 4,

            shape =
                RoundedCornerShape(24.dp)
        )

        Spacer(
            Modifier.width(8.dp)
        )

        FloatingActionButton(
            onClick = onMic,

            containerColor =
                if (listening) {
                    Color(0xFFE53935)
                } else {
                    Color(0xFF5865F2)
                }
        ) {

            Icon(
                imageVector =
                    if (listening) {
                        Icons.Default.Stop
                    } else {
                        Icons.Default.Mic
                    },

                contentDescription =
                    "Voice input"
            )
        }

        Spacer(
            Modifier.width(6.dp)
        )

        Button(
            onClick = onSend,

            modifier =
                Modifier.height(56.dp),

            shape =
                RoundedCornerShape(20.dp)
        ) {

            Icon(
                Icons.Default.Send,
                contentDescription =
                    "Send"
            )
        }
    }
}
