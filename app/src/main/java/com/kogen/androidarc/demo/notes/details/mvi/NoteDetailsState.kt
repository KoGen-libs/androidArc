package com.kogen.androidarc.demo.notes.details.mvi

import com.kogen.androidarc.demo.notes.domain.model.Note
import com.kogen.androidarc.mvi.UiState

data class NoteDetailsState(
    val note: Note? = null,
) : UiState
