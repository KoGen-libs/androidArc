package com.kogen.androidarc.demo.notes.domain.useCases

import com.kogen.androidarc.demo.notes.domain.model.Note
import com.kogen.androidarc.demo.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kz.evko.kogen_di.annotations.KoGenComponent

/** Use case wrapping [NoteRepository.observeNotes] for [NoteListViewModel][com.kogen.androidarc.demo.notes.list.mvi.NoteListViewModel]. */
interface GetNotesUseCase {
    fun execute(): Flow<List<Note>>
}

@KoGenComponent
internal class GetNotesUseCaseImpl(
    private val repository: NoteRepository,
) : GetNotesUseCase {
    override fun execute(): Flow<List<Note>> = repository.observeNotes()
}
