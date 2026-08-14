package com.kogen.androidarc.demo.notes.details.mvi

import com.kogen.androidarc.mvi.UiAction

/** User/lifecycle-triggered intents on the note details screen. */
sealed interface NoteDetailsAction : UiAction {
    data class LoadNote(val id: String) : NoteDetailsAction
    data object NavigateBack : NoteDetailsAction
}
