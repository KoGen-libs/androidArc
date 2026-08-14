[![Maven Central](https://img.shields.io/maven-central/v/io.github.eugenprog/androidarc)](https://central.sonatype.com/artifact/io.github.eugenprog/androidarc)

# androidArc: User Guide

**androidArc** is a minimal MVI (Model-View-Intent) architecture layer for Android + Jetpack Compose - a `ViewModel` base class plus the Compose glue that wires it to a screen. No DI framework, no navigation library, no code generation - just the two building blocks every MVI screen needs.

[Читать на русском](README.ru.md)

**Core Principles:**
* **Tiny surface:** two classes - `BaseMviViewModel` and `ScreenContainerWrapper`. Nothing else to learn.
* **Crash-safe by construction:** an action handler or a background coroutine that throws is logged and swallowed, never propagated to the host app.
* **Unopinionated:** works with whatever DI and navigation you already use - androidArc only depends on `androidx.lifecycle` and Compose runtime.

---

## 🚀 Installation

```kotlin
dependencies {
    // Check the badge above for the latest version
    implementation("io.github.eugenprog:androidarc:<version>")
}
```

---

## ⚙️ How to Use

### 1. Define the screen's contract

```kotlin
sealed interface CounterAction : UiAction {
    data object Increment : CounterAction
}

data class CounterState(val count: Int = 0) : UiState

sealed interface CounterEffect : UiEffect
```

### 2. Implement the ViewModel

```kotlin
class CounterViewModel : BaseMviViewModel<CounterAction, CounterState, CounterEffect>(CounterState()) {
    override fun handleAction(action: CounterAction) {
        when (action) {
            is CounterAction.Increment -> updateState { it.copy(count = it.count + 1) }
        }
    }
}
```

`updateState` drives `state: StateFlow<CounterState>`. For one-shot events (navigation, snackbars), emit through `emitEffect` and collect `effects: Flow<CounterEffect>` instead. `wrappedRequest` runs a suspend call on IO and delivers the result back on Main - the usual shape for a use-case-backed action - and returns the `Job` it launched, so you can cancel that one request individually (e.g. superseding a previous search as a new one starts). Every `wrappedRequest` job is also tracked internally; `cancelAllRequests()` cancels all of them at once, e.g. from `onCleared()`. `launchSafely` is a `viewModelScope.launch` that logs instead of crashing on an uncaught exception, same as `dispatch` does for `handleAction` itself.

### 3. Wire it to a screen

```kotlin
@Composable
fun CounterContainer(viewModel: CounterViewModel = viewModel()) {
    ScreenContainerWrapper(
        viewModel = viewModel,
        onEffect = { /* handle CounterEffect, e.g. navigate */ },
        screenContent = { state, action ->
            CounterScreen(state = state, action = action)
        },
    )
}
```

`ScreenContainerWrapper` collects `state` lifecycle-aware and forwards `effects` to `onEffect`, then renders `screenContent` with `(state, dispatch)`.

---

## ✨ Demo app

The `app` module is a small two-screen "Notes" app (list → details) that puts androidArc through its paces wired up with real DI ([KoGen DI](https://github.com/KoGen-libs/KoGen-Di)) and real navigation ([KoGen Navigation](https://github.com/KoGen-libs/KoGen-Navigation)) code generation - showing that androidArc itself stays agnostic to both. Run it with:

```
./gradlew :app:installDebug
```

[README.ru](README.ru.md)
