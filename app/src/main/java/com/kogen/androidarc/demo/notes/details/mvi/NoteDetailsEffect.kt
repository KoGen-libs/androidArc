package com.kogen.androidarc.demo.notes.details.mvi

import com.kogen.androidarc.mvi.UiEffect

/** One-shot effects emitted by [NoteDetailsViewModel]. */
sealed interface NoteDetailsEffect : UiEffect {
    data object NavigateBack : NoteDetailsEffect
}
