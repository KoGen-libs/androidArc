package com.kogen.androidarc.demo.notes.list.screen

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.kogen.androidarc.demo.di.koGenViewModel
import com.kogen.androidarc.demo.navigation.ActionToNoteDetails
import com.kogen.androidarc.demo.navigation.navigateSafety
import com.kogen.androidarc.demo.notes.list.mvi.NoteListEffect
import com.kogen.androidarc.demo.notes.list.mvi.NoteListViewModel
import com.kogen.androidarc.ui.ScreenContainerWrapper
import kz.evko.navigation.annotation.KoGenScreen

/** Nav-graph start destination: the note list, backed by androidArc's [ScreenContainerWrapper]. */
@KoGenScreen(startDestination = true)
@Composable
internal fun NoteListContainer(
    navController: NavHostController,
) {
    ScreenContainerWrapper(
        viewModel = koGenViewModel<NoteListViewModel>(),
        onEffect = {
            when (it) {
                is NoteListEffect.NavigateToDetails -> {
                    navController.navigateSafety(ActionToNoteDetails(it.id))
                }
            }
        },
        screenContent = { state, action ->
            NoteListScreen(state = state, action = action)
        },
    )
}
