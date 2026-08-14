package com.kogen.androidarc.demo.notes.list.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kogen.androidarc.demo.notes.domain.model.Note
import com.kogen.androidarc.demo.notes.list.mvi.NoteListAction
import com.kogen.androidarc.demo.notes.list.mvi.NoteListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteListScreen(
    state: NoteListState,
    action: (NoteListAction) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("androidArc demo - Notes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { action(NoteListAction.AddRandomNote) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add note")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.notes, key = { it.id }) { note ->
                NoteRow(note = note, onClick = { action(NoteListAction.OpenNote(note.id)) })
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = note.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(
                text = note.content,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
        }
    }
}
