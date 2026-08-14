package com.kogen.androidarc.demo.notes.domain.repository

import com.kogen.androidarc.demo.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kz.evko.kogen_di.annotations.KoGenComponent

/** In-memory notes store - stands in for whatever real persistence a production app would use. */
interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: String): Note?
    suspend fun addNote(title: String, content: String)
}

@KoGenComponent(true)
internal class NoteRepositoryImpl : NoteRepository {

    private val notes = MutableStateFlow(seedNotes())

    override fun observeNotes(): Flow<List<Note>> = notes.asStateFlow()

    override suspend fun getNoteById(id: String): Note? = notes.value.find { it.id == id }

    override suspend fun addNote(title: String, content: String) {
        notes.update { current ->
            current + Note(
                id = "note-${current.size + 1}-${System.currentTimeMillis()}",
                title = title,
                content = content,
                createdAt = System.currentTimeMillis(),
            )
        }
    }

    private companion object {
        fun seedNotes(): List<Note> = listOf(
            Note(
                id = "note-1",
                title = "Welcome to androidArc",
                content = "This note list is androidArc's BaseMviViewModel + ScreenContainerWrapper " +
                    "wired to KoGen DI and KoGen Navigation, exactly like Giraffe's own screens are.",
                createdAt = System.currentTimeMillis(),
            ),
            Note(
                id = "note-2",
                title = "How the list screen works",
                content = "NoteListViewModel extends BaseMviViewModel<NoteListAction, NoteListState, " +
                    "NoteListEffect> and streams notes from GetNotesUseCase. Tapping a note dispatches " +
                    "an action that emits a NavigateToDetails effect, which the container turns into " +
                    "real navigation.",
                createdAt = System.currentTimeMillis(),
            ),
            Note(
                id = "note-3",
                title = "How the details screen works",
                content = "NoteDetailsContainer dispatches LoadNote(id) as soon as it's composed, " +
                    "same pattern Giraffe's own ChatDetailsContainer uses to pass a nav argument into " +
                    "its ViewModel.",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}
