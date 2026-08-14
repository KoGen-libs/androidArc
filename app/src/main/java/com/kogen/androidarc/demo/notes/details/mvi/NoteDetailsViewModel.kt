package com.kogen.androidarc.demo.notes.details.mvi

import com.kogen.androidarc.demo.notes.domain.useCases.GetNoteByIdUseCase
import com.kogen.androidarc.mvi.BaseMviViewModel
import kz.evko.kogen_di.annotations.KoGenViewModel

/** ViewModel for the note details screen: loads the note passed in as a nav argument. */
@KoGenViewModel
internal class NoteDetailsViewModel(
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
) : BaseMviViewModel<NoteDetailsAction, NoteDetailsState, NoteDetailsEffect>(NoteDetailsState()) {

    override fun handleAction(action: NoteDetailsAction) {
        when (action) {
            is NoteDetailsAction.LoadNote -> {
                wrappedRequest(
                    call = { getNoteByIdUseCase.execute(action.id) },
                    onSuccess = { note -> updateState { it.copy(note = note) } },
                )
            }

            is NoteDetailsAction.NavigateBack -> {
                emitEffect(NoteDetailsEffect.NavigateBack)
            }
        }
    }
}
