package com.kogen.androidarc.demo.notes.list.mvi

import com.kogen.androidarc.demo.notes.domain.model.Note
import com.kogen.androidarc.mvi.UiState

data class NoteListState(
    val notes: List<Note> = emptyList(),
) : UiState
