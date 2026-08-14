package com.kogen.androidarc.demo.notes.list.mvi

import com.kogen.androidarc.mvi.UiEffect

/** One-shot effects emitted by [NoteListViewModel]. */
sealed interface NoteListEffect : UiEffect {
    data class NavigateToDetails(val id: String) : NoteListEffect
}
