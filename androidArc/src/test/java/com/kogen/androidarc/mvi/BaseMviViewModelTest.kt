package com.kogen.androidarc.mvi

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

private sealed interface TestAction : UiAction {
    data class RunSuccess(val value: Int) : TestAction
    data object RunFailure : TestAction
    data object RunSuccessNoFinalCallback : TestAction
    data object RunSlow : TestAction
    data object CancelAll : TestAction
    data object Crash : TestAction
    data object Ping : TestAction
}

private data class TestState(
    val value: Int = 0,
    val error: String? = null,
    val loading: Boolean = false,
    val finalCount: Int = 0,
) : UiState

private sealed interface TestEffect : UiEffect {
    data object Pong : TestEffect
}

/**
 * Reads the private `requestJobs` list [wrappedRequest] tracks its jobs in - there's no public
 * API for this (nor should there be, it's an implementation detail), but it's the only way to
 * verify from outside the class that completed requests actually get cleaned up instead of
 * accumulating for the lifetime of the ViewModel.
 */
@Suppress("UNCHECKED_CAST")
private fun BaseMviViewModel<*, *, *>.trackedRequestJobCount(): Int {
    val field = BaseMviViewModel::class.java.getDeclaredField("requestJobs")
    field.isAccessible = true
    return (field.get(this) as List<Job>).size
}

/** Polls [condition] until it's true or [timeoutMs] elapses, then asserts it one last time (so a timeout still fails with a normal assertion message) - [wrappedRequest] hops onto the real `Dispatchers.IO`, so tests can't just `advanceUntilIdle()` to reach quiescence. */
private fun awaitTrue(timeoutMs: Long = 2_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline && !condition()) {
        Thread.sleep(10)
    }
    assertThat(condition()).isTrue()
}

/**
 * Minimal concrete [BaseMviViewModel] driving every action through [wrappedRequest] in some
 * shape - [slowGate], when provided, lets a test hold [TestAction.RunSlow]'s request open (e.g.
 * to cancel it mid-flight) instead of it completing immediately. [onFinalHook] gives tests a
 * dispatcher-agnostic signal for when an async callback has actually run, since [wrappedRequest]
 * hops onto the real `Dispatchers.IO`, which the test dispatcher below doesn't control.
 */
private class TestViewModel(
    private val slowGate: CompletableDeferred<Unit>? = null,
    private val onFinalHook: () -> Unit = {},
    // Deliberately defaults to Dispatchers.IO, same as wrappedRequest itself - only the dispatcher
    // param's own dedicated test below overrides this, to prove the override actually reaches
    // wrappedRequest instead of a hardcoded Dispatchers.IO silently winning anyway.
    private val requestDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseMviViewModel<TestAction, TestState, TestEffect>(TestState()) {

    var lastJob: Job? = null
        private set

    override fun handleAction(action: TestAction) {
        when (action) {
            is TestAction.RunSuccess -> {
                lastJob = wrappedRequest(
                    call = { action.value },
                    onSuccess = { value -> updateState { it.copy(value = value) } },
                    onFinal = { updateState { it.copy(finalCount = it.finalCount + 1) }; onFinalHook() },
                    dispatcher = requestDispatcher,
                )
            }

            is TestAction.RunFailure -> {
                lastJob = wrappedRequest(
                    call = { error("boom") },
                    onError = { e -> updateState { it.copy(error = e.message) } },
                    onFinal = { updateState { it.copy(finalCount = it.finalCount + 1) }; onFinalHook() },
                )
            }

            is TestAction.RunSuccessNoFinalCallback -> {
                // Deliberately omits onFinal entirely, to prove the default no-op doesn't break
                // anything - onSuccess itself signals completion for this one test.
                lastJob = wrappedRequest(
                    call = { 7 },
                    onSuccess = { value -> updateState { it.copy(value = value) }; onFinalHook() },
                )
            }

            is TestAction.RunSlow -> {
                updateState { it.copy(loading = true) }
                // onFinal - not onSuccess - is what hides the loader: it's the only callback
                // guaranteed to run whether this request completes, fails, or gets cancelled.
                lastJob = wrappedRequest(
                    call = { slowGate?.await() },
                    onFinal = {
                        updateState { it.copy(loading = false, finalCount = it.finalCount + 1) }
                        onFinalHook()
                    },
                )
            }

            is TestAction.CancelAll -> cancelAllRequests()

            is TestAction.Crash -> error("boom in handleAction")

            is TestAction.Ping -> emitEffect(TestEffect.Pong)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseMviViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `dispatch swallows an exception thrown by handleAction instead of crashing`() = runTest(dispatcher) {
        val vm = TestViewModel()

        vm.dispatch(TestAction.Crash) // must not throw
    }

    @Test
    fun `wrappedRequest delivers its result through onSuccess and runs onFinal`() = runTest(dispatcher) {
        val latch = CountDownLatch(1)
        val vm = TestViewModel(onFinalHook = { latch.countDown() })

        vm.dispatch(TestAction.RunSuccess(42))

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(vm.state.value.value).isEqualTo(42)
        assertThat(vm.state.value.finalCount).isEqualTo(1)
        // onFinalHook (which the latch above waits on) runs inside the finally block, a moment
        // before the Job itself actually reaches Completed - poll rather than assert immediately.
        awaitTrue { vm.lastJob?.isCompleted == true }
    }

    @Test
    fun `wrappedRequest delivers a failure through onError and still runs onFinal`() = runTest(dispatcher) {
        val latch = CountDownLatch(1)
        val vm = TestViewModel(onFinalHook = { latch.countDown() })

        vm.dispatch(TestAction.RunFailure)

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(vm.state.value.error).isEqualTo("boom")
        assertThat(vm.state.value.finalCount).isEqualTo(1)
    }

    @Test
    fun `onFinal is optional - omitting it does not break onSuccess`() = runTest(dispatcher) {
        val latch = CountDownLatch(1)
        val vm = TestViewModel(onFinalHook = { latch.countDown() })

        vm.dispatch(TestAction.RunSuccessNoFinalCallback)

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        assertThat(vm.state.value.value).isEqualTo(7)
        // No onFinal was passed for this request, so it never touched finalCount.
        assertThat(vm.state.value.finalCount).isEqualTo(0)
    }

    @Test
    fun `cancelAllRequests cancels an in-flight request, runs onFinal, and never calls onError`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val latch = CountDownLatch(1)
            val vm = TestViewModel(slowGate = gate, onFinalHook = { latch.countDown() })

            vm.dispatch(TestAction.RunSlow)
            assertThat(vm.state.value.loading).isTrue()

            vm.dispatch(TestAction.CancelAll)

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
            // Same reasoning as the onFinal/isCompleted race above: poll rather than assert
            // immediately - the Job reaches its final Cancelled state slightly after onFinalHook.
            awaitTrue { vm.lastJob?.isCancelled == true }
            assertThat(vm.state.value.loading).isFalse() // onFinal still hid the loader
            assertThat(vm.state.value.error).isNull() // cancellation is not a reported failure
            assertThat(vm.state.value.finalCount).isEqualTo(1)
        }

    @Test
    fun `a completed request is removed from the tracked job list, not left behind`() = runTest(dispatcher) {
        val latch = CountDownLatch(1)
        val vm = TestViewModel(onFinalHook = { latch.countDown() })

        vm.dispatch(TestAction.RunSuccess(1))

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
        // onFinal (which the latch above waits on) runs just before the job itself finishes, so
        // the internal cleanup can still land a moment after the latch fires - poll rather than
        // assert immediately.
        awaitTrue { vm.trackedRequestJobCount() == 0 }
    }

    @Test
    fun `an in-flight request stays tracked until it completes, then is removed`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val vm = TestViewModel(slowGate = gate)

        vm.dispatch(TestAction.RunSlow)
        assertThat(vm.trackedRequestJobCount()).isEqualTo(1) // added synchronously by wrappedRequest

        gate.complete(Unit)

        awaitTrue { vm.trackedRequestJobCount() == 0 }
    }

    @Test
    fun `emitted effects are delivered through the effects flow`() = runTest(dispatcher) {
        val vm = TestViewModel()

        vm.effects.test {
            vm.dispatch(TestAction.Ping)
            assertThat(awaitItem()).isEqualTo(TestEffect.Pong)
        }
    }

    @Test
    fun `wrappedRequest's dispatcher param overrides the Dispatchers-IO default`() = runTest(dispatcher) {
        // The same UnconfinedTestDispatcher already driving this whole test, passed straight
        // through to wrappedRequest - unlike every other test here, this needs no latch/awaitTrue
        // polling at all: with a real Dispatchers.IO hop there's no way to observe completion
        // synchronously, but an UnconfinedTestDispatcher runs its work eagerly, inline, so the
        // request has already finished by the time dispatch() returns.
        val vm = TestViewModel(requestDispatcher = dispatcher)

        vm.dispatch(TestAction.RunSuccess(42))

        assertThat(vm.state.value.value).isEqualTo(42)
        assertThat(vm.state.value.finalCount).isEqualTo(1)
    }

    @Test
    fun `awaitIdle suspends until every tracked wrappedRequest job has finished`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val vm = TestViewModel(slowGate = gate)

        vm.dispatch(TestAction.RunSlow)
        assertThat(vm.state.value.loading).isTrue() // still in flight

        gate.complete(Unit)
        vm.awaitIdle()

        // No latch, no awaitTrue polling needed here either - awaitIdle itself is the
        // synchronization point, which is the entire point of adding it: generated test
        // scaffolding (or any other caller outside this class) can await this instead of guessing
        // whether the action it just dispatched happened to be synchronous or not.
        assertThat(vm.state.value.loading).isFalse()
        assertThat(vm.state.value.finalCount).isEqualTo(1)
        assertThat(vm.trackedRequestJobCount()).isEqualTo(0)
    }

    @Test
    fun `awaitIdle returns immediately when nothing is tracked`() = runTest(dispatcher) {
        val vm = TestViewModel()

        vm.awaitIdle() // must not hang - nothing was ever dispatched
    }
}
