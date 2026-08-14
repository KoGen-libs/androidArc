package com.kogen.androidarc.demo.notes.details.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.kogen.androidarc.demo.di.koGenViewModel
import com.kogen.androidarc.demo.navigation.popBackSafety
import com.kogen.androidarc.demo.notes.details.mvi.NoteDetailsAction
import com.kogen.androidarc.demo.notes.details.mvi.NoteDetailsEffect
import com.kogen.androidarc.demo.notes.details.mvi.NoteDetailsViewModel
import com.kogen.androidarc.ui.ScreenContainerWrapper
import kz.evko.navigation.annotation.KoGenScreen

/** Nav-graph destination for a single note's details. Loads [noteId] as soon as it's composed. */
@KoGenScreen
@Composable
internal fun NoteDetailsContainer(
    navController: NavHostController,
    noteId: String,
) {
    ScreenContainerWrapper(
        viewModel = koGenViewModel<NoteDetailsViewModel>(),
        onEffect = {
            when (it) {
                is NoteDetailsEffect.NavigateBack -> navController.popBackSafety()
            }
        },
        screenContent = { state, action ->
            LaunchedEffect(noteId) {
                action(NoteDetailsAction.LoadNote(noteId))
            }

            NoteDetailsScreen(state = state, action = action)
        },
    )
}
