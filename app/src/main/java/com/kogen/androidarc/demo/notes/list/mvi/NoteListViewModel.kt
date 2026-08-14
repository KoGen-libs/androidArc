package com.kogen.androidarc.demo.notes.list.mvi

import com.kogen.androidarc.demo.notes.domain.useCases.AddNoteUseCase
import com.kogen.androidarc.demo.notes.domain.useCases.GetNotesUseCase
import com.kogen.androidarc.mvi.BaseMviViewModel
import kz.evko.kogen_di.annotations.KoGenViewModel

/** ViewModel for the note list screen: streams all notes and lets the user open one or add a new one. */
@KoGenViewModel
internal class NoteListViewModel(
    private val getNotesUseCase: GetNotesUseCase,
    private val addNoteUseCase: AddNoteUseCase,
) : BaseMviViewModel<NoteListAction, NoteListState, NoteListEffect>(NoteListState()) {

    init {
        launchSafely {
            getNotesUseCase.execute().collect { notes ->
                updateState { it.copy(notes = notes) }
            }
        }
    }

    override fun handleAction(action: NoteListAction) {
        when (action) {
            is NoteListAction.OpenNote -> {
                emitEffect(NoteListEffect.NavigateToDetails(action.id))
            }

            is NoteListAction.AddRandomNote -> {
                val count = state.value.notes.size + 1
                wrappedRequest(
                    call = {
                        addNoteUseCase.execute(
                            "Note #$count",
                            "Added at ${System.currentTimeMillis()} - proof the list re-renders " +
                                "off a plain updateState { } call inside BaseMviViewModel.",
                        )
                    },
                )
            }
        }
    }
}
