package com.kogen.androidarc.demo.notes.domain.useCases

import com.kogen.androidarc.demo.notes.domain.repository.NoteRepository
import kz.evko.kogen_di.annotations.KoGenComponent

/** Use case wrapping [NoteRepository.addNote] for [NoteListViewModel][com.kogen.androidarc.demo.notes.list.mvi.NoteListViewModel]. */
interface AddNoteUseCase {
    suspend fun execute(title: String, content: String)
}

@KoGenComponent
internal class AddNoteUseCaseImpl(
    private val repository: NoteRepository,
) : AddNoteUseCase {
    override suspend fun execute(title: String, content: String) = repository.addNote(title, content)
}
