package com.example.timetable

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class TimetableEntry(
    val id: String = UUID.randomUUID().toString(),
    val day: DayOfWeek,
    val subject: String,
    val teacher: String,
    val room: String,
    val startTime: String,
    val endTime: String
)

class TimetableStorage(context: Context) {

    private val prefs = context.getSharedPreferences(
        "timetable",
        Context.MODE_PRIVATE
    )

    fun load(): List<TimetableEntry> {
        val raw = prefs.getString("entries", null) ?: return emptyList()

        return try {
            val array = JSONArray(raw)

            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    add(
                        TimetableEntry(
                            id = obj.getString("id"),
                            day = DayOfWeek.valueOf(obj.getString("day")),
                            subject = obj.getString("subject"),
                            teacher = obj.getString("teacher"),
                            room = obj.getString("room"),
                            startTime = obj.getString("startTime"),
                            endTime = obj.getString("endTime")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(entries: List<TimetableEntry>) {
        val array = JSONArray()

        entries.forEach { entry ->
            val obj = JSONObject()

            obj.put("id", entry.id)
            obj.put("day", entry.day.name)
            obj.put("subject", entry.subject)
            obj.put("teacher", entry.teacher)
            obj.put("room", entry.room)
            obj.put("startTime", entry.startTime)
            obj.put("endTime", entry.endTime)

            array.put(obj)
        }

        prefs.edit()
            .putString("entries", array.toString())
            .apply()
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storage = TimetableStorage(this)

        setContent {
            TimetableTheme {
                TimetableApp(storage)
            }
        }
    }
}

@Composable
fun TimetableTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableApp(storage: TimetableStorage) {

    var entries by remember {
        mutableStateOf(storage.load())
    }

    var selectedDay by remember {
        mutableStateOf(
            LocalDate.now().dayOfWeek
        )
    }

    var showEditor by remember {
        mutableStateOf(false)
    }

    var editingEntry by remember {
        mutableStateOf<TimetableEntry?>(null)
    }

    val dayEntries = entries
        .filter { it.day == selectedDay }
        .sortedBy {
            parseTime(it.startTime)
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Timetable",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Weekly class schedule",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEntry = null
                    showEditor = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add class"
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            DaySelector(
                selectedDay = selectedDay,
                onDaySelected = {
                    selectedDay = it
                }
            )

            if (dayEntries.isEmpty()) {

                EmptySchedule(
                    onAdd = {
                        editingEntry = null
                        showEditor = true
                    }
                )

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = dayEntries,
                        key = { it.id }
                    ) { entry ->

                        TimetableCard(
                            entry = entry,

                            onEdit = {
                                editingEntry = entry
                                showEditor = true
                            },

                            onDelete = {

                                entries = entries.filter {
                                    it.id != entry.id
                                }

                                storage.save(entries)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {

        EntryEditorDialog(
            existing = editingEntry,
            selectedDay = selectedDay,

            onDismiss = {
                showEditor = false
            },

            onSave = { entry ->

                entries = if (editingEntry == null) {
                    entries + entry
                } else {
                    entries.map {
                        if (it.id == entry.id) entry else it
                    }
                }

                storage.save(entries)

                selectedDay = entry.day
                showEditor = false
            }
        )
    }
}

@Composable
fun DaySelector(
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit
) {

    val days = DayOfWeek.values()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 10.dp
        )
    ) {

        item {

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                days.forEach { day ->

                    val selected = day == selectedDay

                    Surface(
                        modifier = Modifier
                            .width(54.dp)
                            .height(48.dp)
                            .clickable {
                                onDaySelected(day)
                            },

                        shape = RoundedCornerShape(14.dp),

                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text(
                                text = day.name.take(3),
                                fontWeight = FontWeight.Bold,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableCard(
    entry: TimetableEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val isCurrent = isCurrentClass(entry)

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = entry.subject,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "${entry.startTime} – ${entry.endTime}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (entry.teacher.isNotBlank()) {

                Text(
                    text = "Teacher: ${entry.teacher}"
                )
            }

            if (entry.room.isNotBlank()) {

                Text(
                    text = "Room: ${entry.room}"
                )
            }

            if (isCurrent) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "● CURRENT CLASS",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptySchedule(
    onAdd: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "No classes today",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Add a class to your timetable."
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = onAdd
            ) {
                Text("Add class")
            }
        }
    }
}

@Composable
fun EntryEditorDialog(
    existing: TimetableEntry?,
    selectedDay: DayOfWeek,
    onDismiss: () -> Unit,
    onSave: (TimetableEntry) -> Unit
) {

    var day by remember {
        mutableStateOf(
            existing?.day ?: selectedDay
        )
    }

    var subject by remember {
        mutableStateOf(existing?.subject ?: "")
    }

    var teacher by remember {
        mutableStateOf(existing?.teacher ?: "")
    }

    var room by remember {
        mutableStateOf(existing?.room ?: "")
    }

    var startTime by remember {
        mutableStateOf(existing?.startTime ?: "09:00")
    }

    var endTime by remember {
        mutableStateOf(existing?.endTime ?: "10:00")
    }

    var error by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text(
                if (existing == null) {
                    "Add class"
                } else {
                    "Edit class"
                }
            )
        },

        text = {

            Column {

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                    },
                    label = {
                        Text("Subject")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = teacher,
                    onValueChange = {
                        teacher = it
                    },
                    label = {
                        Text("Teacher")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = {
                        room = it
                    },
                    label = {
                        Text("Room")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = startTime,
                    onValueChange = {
                        startTime = it
                    },
                    label = {
                        Text("Start time")
                    },
                    placeholder = {
                        Text("09:00")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = {
                        endTime = it
                    },
                    label = {
                        Text("End time")
                    },
                    placeholder = {
                        Text("10:00")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text = "Day",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    listOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY
                    ).forEach { option ->

                        DayButton(
                            day = option,
                            selected = day == option,
                            onClick = {
                                day = option
                            }
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    listOf(
                        DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    ).forEach { option ->

                        DayButton(
                            day = option,
                            selected = day == option,
                            onClick = {
                                day = option
                            }
                        )
                    }
                }

                if (error.isNotBlank()) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (subject.isBlank()) {
                        error = "Enter a subject."
                        return@Button
                    }

                    val start = parseTime(startTime)
                    val end = parseTime(endTime)

                    if (start == null || end == null) {
                        error = "Use time format HH:MM, for example 09:30."
                        return@Button
                    }

                    if (!end.isAfter(start)) {
                        error = "End time must be after start time."
                        return@Button
                    }

                    onSave(
                        TimetableEntry(
                            id = existing?.id
                                ?: UUID.randomUUID().toString(),

                            day = day,
                            subject = subject.trim(),
                            teacher = teacher.trim(),
                            room = room.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim()
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DayButton(
    day: DayOfWeek,
    selected: Boolean,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick
    ) {

        Text(
            text = day.name.take(3),
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Unspecified
            }
        )
    }
}

fun parseTime(value: String): LocalTime? {

    return try {
        LocalTime.parse(value)
    } catch (_: Exception) {
        null
    }
}

fun isCurrentClass(entry: TimetableEntry): Boolean {

    val now = LocalTime.now()

    val start = parseTime(entry.startTime) ?: return false
    val end = parseTime(entry.endTime) ?: return false

    return LocalDate.now().dayOfWeek == entry.day &&
            !now.isBefore(start) &&
            now.isBefore(end)
}
