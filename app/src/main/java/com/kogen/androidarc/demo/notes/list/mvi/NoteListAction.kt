package com.kogen.androidarc.demo.notes.list.mvi

import com.kogen.androidarc.mvi.UiAction

/** User-triggered intents on the note list screen. */
sealed interface NoteListAction : UiAction {
    data class OpenNote(val id: String) : NoteListAction
    data object AddRandomNote : NoteListAction
}
