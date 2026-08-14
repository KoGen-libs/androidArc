package com.kogen.androidarc.demo.notes.domain.useCases

import com.kogen.androidarc.demo.notes.domain.model.Note
import com.kogen.androidarc.demo.notes.domain.repository.NoteRepository
import kz.evko.kogen_di.annotations.KoGenComponent

/** Use case wrapping [NoteRepository.getNoteById] for [NoteDetailsViewModel][com.kogen.androidarc.demo.notes.details.mvi.NoteDetailsViewModel]. */
interface GetNoteByIdUseCase {
    suspend fun execute(id: String): Note?
}

@KoGenComponent
internal class GetNoteByIdUseCaseImpl(
    private val repository: NoteRepository,
) : GetNoteByIdUseCase {
    override suspend fun execute(id: String): Note? = repository.getNoteById(id)
}
