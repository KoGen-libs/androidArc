# androidArc

A minimal MVI (Model-View-Intent) architecture layer for Android + Jetpack Compose, extracted
from [Giraffe](https://github.com/KoGen-libs/Giraffe)'s internal UI layer into a standalone,
reusable artifact.

It's deliberately small - two files:

- **`BaseMviViewModel<Action, State, Effect>`** - a `ViewModel` base class that holds a single
  `StateFlow<State>` and a buffered `Flow<Effect>` for one-shot events (navigation, snackbars,
  etc.). Subclasses implement `handleAction`, and drive state/effects via `updateState` /
  `emitEffect`. Both `dispatch(action)` and any coroutine started with `launchSafely` swallow and
  log uncaught exceptions instead of crashing the host app.
- **`ScreenContainerWrapper`** - the standard Compose glue between a `BaseMviViewModel` and its
  screen: collects state lifecycle-aware, forwards effects to a callback, and renders the screen
  with `(state, dispatch)`.

## Install

```kotlin
dependencies {
    implementation("io.github.eugenprog:androidarc:<version>")
}
```

## Usage

```kotlin
// 1. Define your screen's contract
sealed interface CounterAction : UiAction {
    data object Increment : CounterAction
}
data class CounterState(val count: Int = 0) : UiState
sealed interface CounterEffect : UiEffect

// 2. Implement the ViewModel
class CounterViewModel : BaseMviViewModel<CounterAction, CounterState, CounterEffect>(CounterState()) {
    override fun handleAction(action: CounterAction) {
        when (action) {
            is CounterAction.Increment -> updateState { it.copy(count = it.count + 1) }
        }
    }
}

// 3. Wire it to a screen
@Composable
fun CounterContainer(viewModel: CounterViewModel = viewModel()) {
    ScreenContainerWrapper(
        viewModel = viewModel,
        screenContent = { state, action ->
            CounterScreen(state = state, action = action)
        },
    )
}
```

Nothing here is tied to any particular DI or navigation library - `BaseMviViewModel` and
`ScreenContainerWrapper` only depend on `androidx.lifecycle` and Compose runtime.

## Demo app

The `app` module is a small two-screen "Notes" app that puts `androidArc` through its paces the
same way [Giraffe](https://github.com/KoGen-libs/Giraffe) uses it internally: screens built on
`BaseMviViewModel` + `ScreenContainerWrapper`, wired up with
[KoGen DI](https://github.com/EugenProg/AndroidDi) (`@KoGenComponent` / `@KoGenViewModel`) and
[KoGen Navigation](https://github.com/EugenProg/AndroidNavigation) (`@KoGenScreen`) - both
code-gen libraries auto-generate the DI graph and nav graph from those annotations, so there's no
manual wiring beyond declaring the screens and use cases.

- **Note list** - streams notes from an in-memory repository via a use case; tapping a note or
  tapping the FAB dispatches an action, one of which emits a `NavigateToDetails` effect.
- **Note details** - receives a nav argument, dispatches a `LoadNote` action for it on first
  composition (the same pattern Giraffe's own `ChatDetailsContainer` uses), and can navigate back.

Run it with:

```
./gradlew :app:installDebug
```

## Publishing

Released to Maven Central under `io.github.eugenprog:androidarc`, following the same
JReleaser + Sonatype Central Portal setup as Giraffe (see `jreleaser.yml` and
`.github/workflows/release.yml`). Publishing a new version:

1. Bump the version and push a tag matching it (tag creation is restricted to the Admin role).
2. The `Publish to mavencentral` workflow builds, signs, and deploys via JReleaser.

This requires the same GPG/Sonatype secrets Giraffe uses
(`JRELEASER_GPG_PUBLIC_KEY`, `JRELEASER_GPG_SECRET_KEY`, `JRELEASER_GPG_PASSPHRASE`,
`JRELEASER_MAVENCENTRAL_SONATYPE_USERNAME`, `JRELEASER_MAVENCENTRAL_SONATYPE_TOKEN`) configured as
repository secrets here as well - GitHub doesn't let secret values be copied between repos.

## License

Apache License 2.0 - see [LICENSE](LICENSE).
