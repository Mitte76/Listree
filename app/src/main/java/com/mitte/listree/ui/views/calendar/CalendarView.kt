package com.mitte.listree.ui.views.calendar

import android.Manifest
import android.content.ContentResolver
import android.provider.CalendarContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

data class CalendarEvent(val title: String, val startMillis: Long, val endMillis: Long)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalendarView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var events by remember { mutableStateOf(listOf<CalendarEvent>()) }

    val permissionState = rememberPermissionState(Manifest.permission.READ_CALENDAR)

    Column(modifier = modifier.padding(16.dp)) {
        if (permissionState.status.isGranted) {
            // Permission granted → fetch events
            LaunchedEffect(Unit) {
                events = fetchCalendarEvents(context.contentResolver)
            }

            Text("Upcoming events:")
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(events) { event ->
                    Text("${event.title} | ${event.startMillis} - ${event.endMillis}")
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            // Permission not granted → show button
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Grant Calendar Permission")
            }
        }
    }
}

fun fetchCalendarEvents(contentResolver: ContentResolver): List<CalendarEvent> {
    val events = mutableListOf<CalendarEvent>()
    val uri = CalendarContract.Events.CONTENT_URI
    val projection = arrayOf(
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND
    )

    val cursor = contentResolver.query(
        uri, projection, null, null,
        CalendarContract.Events.DTSTART + " ASC"
    )

    cursor?.use {
        val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
        val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
        val endIndex = it.getColumnIndex(CalendarContract.Events.DTEND)

        while (it.moveToNext()) {
            val title = it.getString(titleIndex) ?: "No Title"
            val start = it.getLong(startIndex)
            val end = it.getLong(endIndex)
            events.add(CalendarEvent(title, start, end))
        }
    }
    return events
}
