package com.kogen.androidarc.mvi

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Marker for a user/UI-triggered intent dispatched to a [BaseMviViewModel]. */
interface UiAction
/** Marker for a screen's observable state, held by a [BaseMviViewModel]. */
interface UiState
/** Marker for a one-shot side effect (navigation, etc.) emitted by a [BaseMviViewModel]. */
interface UiEffect

/**
 * Base MVI ViewModel meant to be shared by every feature screen in an app: holds a single
 * [state] [kotlinx.coroutines.flow.StateFlow] and emits one-shot [effects] through a buffered
 * channel (so effects like navigation aren't dropped if emitted before a collector attaches, but
 * also aren't replayed to a later collector). Subclasses implement [handleAction] and drive state
 * via [updateState]/[emitEffect], and issue background requests via [wrappedRequest] - every such
 * job is tracked and can be cancelled in bulk via [cancelAllRequests].
 */
abstract class BaseMviViewModel<A : UiAction, S : UiState, E : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * Entry point called by [com.kogen.androidarc.ui.ScreenContainerWrapper] for every
     * UI-dispatched action - typically straight off a Compose click handler, so an uncaught
     * exception here would crash synchronously on the UI thread. [handleAction] is never allowed
     * to propagate: a bug in one screen's action handling shouldn't be able to take the whole app
     * down.
     */
    fun dispatch(action: A) {
        logAction(action)
        try {
            handleAction(action)
        } catch (e: Exception) {
            Log.w("MVI_ERROR", "Unexpected error handling ${action::class.simpleName} - ignoring it", e)
        }
    }

    protected abstract fun handleAction(action: A)

    protected fun updateState(transform: (S) -> S) {
        _state.update(transform)
    }

    protected fun emitEffect(effect: E) {
        logEffect(effect)
        launchSafely { _effects.send(effect) }
    }

    /**
     * Launches [block] on [viewModelScope], swallowing (and logging) any uncaught exception
     * instead of letting it crash the host app - the same reasoning as [dispatch], for a screen's
     * own background work (streaming state from a use case, forwarding an effect, etc.). Use this
     * instead of a bare `viewModelScope.launch` for anything that isn't already its own try/catch
     * (like [wrappedRequest] below).
     */
    protected fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.w("MVI_ERROR", "Unexpected error in a background coroutine - ignoring it", e)
            }
        }
    }

    private fun logAction(action: A) {
        Log.d("MVI_ACTION", "🚀 Action: ${action::class.simpleName}")
    }

    private fun logEffect(effect: E) {
        Log.d("MVI_EFFECT", "✨ Effect: ${effect::class.simpleName}")
    }

    // Thread-safe: entries are appended from whatever thread calls wrappedRequest (normally
    // Main, via dispatch), but removed from each job's own completion callback, which can run on
    // Dispatchers.IO. A plain MutableList would risk a ConcurrentModificationException between
    // those two sides; CopyOnWriteArrayList makes both safe without an explicit lock.
    private val requestJobs = CopyOnWriteArrayList<Job>()

    /**
     * Runs [call] on IO, then delivers its result/failure back on Main via [onSuccess]/[onError] -
     * the standard shape for a use-case-backed action. The returned [Job] is also tracked
     * internally (removed automatically once it completes) so [cancelAllRequests] can cancel every
     * in-flight request at once - callers can additionally hang on to it to cancel that one
     * request individually (e.g. superseding a previous search as a new one starts).
     */
    protected fun <T> wrappedRequest(
        call: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ): Job {
        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = call()
                withContext(Dispatchers.Main) { onSuccess(result) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
        requestJobs += job
        job.invokeOnCompletion { requestJobs -= job }
        return job
    }

    /** Cancels every [wrappedRequest] job still in flight - e.g. from [onCleared] or when a screen navigates away mid-request. */
    protected fun cancelAllRequests() {
        requestJobs.forEach { it.cancel() }
    }
}
